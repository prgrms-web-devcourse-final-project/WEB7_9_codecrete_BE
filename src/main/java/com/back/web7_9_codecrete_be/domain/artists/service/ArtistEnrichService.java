package com.back.web7_9_codecrete_be.domain.artists.service;

import com.back.web7_9_codecrete_be.domain.artists.entity.Artist;
import com.back.web7_9_codecrete_be.domain.artists.repository.ArtistRepository;
import com.back.web7_9_codecrete_be.global.musicbrainz.MusicBrainzClient;
import com.back.web7_9_codecrete_be.global.wikidata.WikidataClient;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArtistEnrichService {

    private final ArtistRepository artistRepository;
    private final MusicBrainzClient musicBrainzClient;
    private final WikidataClient wikidataClient;


    // Wikidata + Wikipedia + MusicBrainz를 통합하여 아티스트 정보를 가져와 enrich를 수행
    public int enrichArtist(int limit) {
        int actualLimit = limit > 0 ? Math.min(limit, 300) : 100;
        List<Artist> targets = artistRepository.findByNameKoIsNullOrderByIdAsc(
                PageRequest.of(0, actualLimit)
        );
        log.info("통합 enrich 시작 (Wikidata + Wikipedia + MusicBrainz): 요청 limit={}, 실제 limit={}, 대상 {}명",
                limit, actualLimit, targets.size());

        if (targets.isEmpty()) {
            log.warn("⚠️ enrich할 대상 아티스트가 없습니다. (모두 이미 enrich되었거나 DB에 아티스트가 없습니다)");
            return 0;
        }
        int updated = 0;
        int failedNotFound = 0;
        int failedException = 0;

        for (Artist artist : targets) {
            try {
                // 각 아티스트마다 별도 트랜잭션으로 처리하여 즉시 커밋
                enrichSingleArtist(artist);
                updated++;

                // API rate limit 고려 (가장 느린 MusicBrainz 기준)
                // InterruptedException을 안전하게 처리
                try {
                    Thread.sleep(1100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("⚠️ Enrich 중 sleep 중단됨 (서버 종료 가능성): 처리된 개수={}", updated);
                    // 이미 처리된 것은 저장되었으므로 break
                    break;
                }

            } catch (RuntimeException e) {
                // 아티스트 정보를 찾을 수 없는 경우
                if (e.getMessage() != null && e.getMessage().contains("아티스트 정보를 찾을 수 없음")) {
                    failedNotFound++;
                } else {
                    log.error("❌ Enrich 예외 발생: artistId={}, name={}, spotifyId={}, error={}",
                            artist.getId(), artist.getArtistName(), artist.getSpotifyArtistId(), e.getMessage(), e);
                    failedException++;
                }
            } catch (Exception e) {
                log.error("❌ Enrich 예외 발생: artistId={}, name={}, spotifyId={}, error={}",
                        artist.getId(), artist.getArtistName(), artist.getSpotifyArtistId(), e.getMessage(), e);
                failedException++;
            }
        }

        int totalFailed = failedNotFound + failedException;
        log.info("📊 통합 enrich 완료: 성공={}, 실패={} (정보없음={}, 예외={}), 총={}",
                updated, totalFailed, failedNotFound, failedException, targets.size());
        return updated;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void enrichSingleArtist(Artist artist) {
        log.debug("Enrich 처리 중: artistId={}, name={}, spotifyId={}",
                artist.getId(), artist.getArtistName(), artist.getSpotifyArtistId());

        EnrichResult result = enrichArtist(artist);

        if (result == null) {
            log.warn("❌ 아티스트 정보를 찾을 수 없음: artistId={}, name={}, spotifyId={}",
                    artist.getId(), artist.getArtistName(), artist.getSpotifyArtistId());
            throw new RuntimeException("아티스트 정보를 찾을 수 없음");
        }

        // 기존 artistType이 있으면 유지, 없으면 가져온 값 사용
        String artistType = result.artistType != null ? result.artistType : artist.getArtistType();

        // ✅ 기존 row를 "보강"
        artist.updateProfile(result.nameKo, result.artistGroup, artistType);
        // 명시적으로 save하여 변경사항을 DB에 즉시 반영
        artistRepository.save(artist);
        log.info("✅ Enrich 성공: artistId={}, name={}, nameKo={}, group={}, type={}, source={}",
                artist.getId(), artist.getArtistName(), result.nameKo,
                result.artistGroup, artistType, result.source);
    }

    private EnrichResult enrichArtist(Artist artist) {
        String nameKo = null;
        String artistGroup = null;
        String artistType = null;
        String source = "";

        // 1단계: Spotify ID로 Wikidata 찾기 (가장 정확)
        Optional<String> qidOpt = wikidataClient.searchWikidataIdBySpotifyId(artist.getSpotifyArtistId());
        if (qidOpt.isEmpty()) {
            // Spotify ID로 못 찾으면 이름으로 시도
            qidOpt = wikidataClient.searchWikidataId(artist.getArtistName());
        }

        if (qidOpt.isPresent()) {
            String qid = qidOpt.get();
            Optional<JsonNode> entityOpt = wikidataClient.getEntityInfo(qid);

            if (entityOpt.isPresent()) {
                JsonNode entity = entityOpt.get();

                // Wikipedia에서 한국어 이름 가져오기
                Optional<String> nameKoOpt = wikidataClient.getKoreanNameFromWikipedia(entity);
                if (nameKoOpt.isPresent()) {
                    nameKo = nameKoOpt.get();
                    source += "Wikipedia ";
                }

                // Wikidata에서 아티스트 타입 추출
                artistType = inferArtistTypeFromWikidata(entity);
                if (artistType != null) {
                    source += "Wikidata ";
                }

                // Wikidata에서 소속 그룹 추출
                artistGroup = resolveGroupNameFromWikidata(entity);
                if (artistGroup != null) {
                    source += "Wikidata ";
                }
            }
        }

        // 2단계: Wikipedia에서 직접 검색 (Wikidata 실패 시)
        if (nameKo == null) {
            Optional<String> nameKoOpt = wikidataClient.searchKoreanNameFromWikipedia(artist.getArtistName());
            if (nameKoOpt.isPresent()) {
                nameKo = nameKoOpt.get();
                source += "Wikipedia ";
            }
        }

        // 3단계: MusicBrainz에서 추가 정보 가져오기 (보완, 실패해도 계속 진행)
        try {
            Optional<MusicBrainzClient.ArtistInfo> mbInfoOpt = musicBrainzClient.searchArtist(artist.getArtistName());
            if (mbInfoOpt.isPresent()) {
                MusicBrainzClient.ArtistInfo mbInfo = mbInfoOpt.get();

                // 한국어 이름이 없으면 MusicBrainz에서 가져오기
                if (nameKo == null && mbInfo.getNameKo() != null) {
                    nameKo = mbInfo.getNameKo();
                    source += "MusicBrainz ";
                }

                // 소속 그룹이 없으면 MusicBrainz에서 가져오기
                if (artistGroup == null && mbInfo.getArtistGroup() != null) {
                    artistGroup = mbInfo.getArtistGroup();
                    source += "MusicBrainz ";
                }

                // 아티스트 타입이 없으면 MusicBrainz에서 가져오기
                if (artistType == null && mbInfo.getArtistType() != null) {
                    artistType = mbInfo.getArtistType();
                    source += "MusicBrainz ";
                }
            }
        } catch (Exception e) {
            // MusicBrainz 실패해도 Wikidata/Wikipedia 정보로는 계속 진행
            log.debug("MusicBrainz 정보 가져오기 실패 (무시하고 계속 진행): name={}, error={}",
                    artist.getArtistName(), e.getMessage());
        }

        // 최소한 한국어 이름은 있어야 성공으로 간주
        if (nameKo == null) {
            return null;
        }

        return new EnrichResult(nameKo, artistGroup, artistType, source.trim());
    }

    //Wikidata 엔티티에서 아티스트 타입 추출
    private String inferArtistTypeFromWikidata(JsonNode entity) {
        // P31 instance of: human(Q5), musical group(Q215380)
        List<String> instanceOfList = wikidataClient.getAllEntityIdClaims(entity, "P31");

        // Q215380 (musical group)이 있으면 GROUP
        if (instanceOfList.contains("Q215380")) {
            return "GROUP";
        }

        // P463 (member of) 속성이 있으면 그룹 멤버이므로 SOLO
        Optional<String> memberOf = wikidataClient.getEntityIdClaim(entity, "P463");
        if (memberOf.isPresent()) {
            return "SOLO";
        }

        // Q5 (human)만 있으면 SOLO
        if (instanceOfList.contains("Q5") && instanceOfList.size() == 1) {
            return "SOLO";
        }

        return null;
    }

    // Wikidata 엔티티에서 소속 그룹 이름 추출
    private String resolveGroupNameFromWikidata(JsonNode artistEntity) {
        // P463 member of
        Optional<String> groupQid = wikidataClient.getEntityIdClaim(artistEntity, "P463");
        if (groupQid.isEmpty()) return null;

        Optional<JsonNode> groupEntityOpt = wikidataClient.getEntityInfo(groupQid.get());
        if (groupEntityOpt.isEmpty()) return null;

        // Wikipedia에서 그룹 이름 가져오기
        return wikidataClient.getKoreanNameFromWikipedia(groupEntityOpt.get()).orElse(null);
    }

    private static class EnrichResult {
        final String nameKo;
        final String artistGroup;
        final String artistType;
        final String source;

        EnrichResult(String nameKo, String artistGroup, String artistType, String source) {
            this.nameKo = nameKo;
            this.artistGroup = artistGroup;
            this.artistType = artistType;
            this.source = source;
        }
    }

}
