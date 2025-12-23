package com.back.web7_9_codecrete_be.domain.artists.service;

import com.back.web7_9_codecrete_be.domain.artists.entity.Artist;
import com.back.web7_9_codecrete_be.domain.artists.entity.ArtistType;
import com.back.web7_9_codecrete_be.domain.artists.repository.ArtistRepository;
import com.back.web7_9_codecrete_be.global.flo.FloClient;
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
    private final FloClient floClient;
    private final MusicBrainzClient musicBrainzClient;
    private final WikidataClient wikidataClient;

    // MusicBrainz ID만 받아오기
    public int fetchMusicBrainzIds(int limit) {
        int actualLimit = limit > 0 ? Math.min(limit, 300) : 100;
        List<Artist> targets = artistRepository.findByMusicBrainzIdIsNullOrderByIdAsc(
                PageRequest.of(0, actualLimit)
        );
        log.info("MusicBrainz ID 수집 시작: 요청 limit={}, 실제 limit={}, 대상 {}명", limit, actualLimit, targets.size());

        if (targets.isEmpty()) {
            log.warn("⚠️ MusicBrainz ID를 수집할 대상 아티스트가 없습니다.");
            return 0;
        }

        int updated = 0;
        int failed = 0;

        for (Artist artist : targets) {
            try {
                fetchMusicBrainzId(artist);
                updated++;

                // API rate limit 고려 (MusicBrainz는 1초에 1회 요청 권장)
                try {
                    Thread.sleep(1100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("⚠️ MusicBrainz ID 수집 중 sleep 중단됨: 처리된 개수={}", updated);
                    break;
                }
            } catch (Exception e) {
                log.error("❌ MusicBrainz ID 수집 예외 발생: artistId={}, name={}, error={}",
                        artist.getId(), artist.getArtistName(), e.getMessage(), e);
                failed++;
            }
        }

        log.info("📊 MusicBrainz ID 수집 완료: 성공={}, 실패={}, 총={}", updated, failed, targets.size());
        return updated;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void fetchMusicBrainzId(Artist artist) {
        // nameKo를 우선 사용, 없으면 artistName 사용
        String searchName = (artist.getNameKo() != null && !artist.getNameKo().isBlank()) 
                ? artist.getNameKo() 
                : artist.getArtistName();
        
        log.debug("MusicBrainz ID 수집 중: artistId={}, nameKo={}, artistName={}, searchName={}",
                artist.getId(), artist.getNameKo(), artist.getArtistName(), searchName);

        try {
            Optional<MusicBrainzClient.ArtistInfo> mbInfoOpt = musicBrainzClient.searchArtist(searchName);
            if (mbInfoOpt.isPresent()) {
                MusicBrainzClient.ArtistInfo mbInfo = mbInfoOpt.get();
                
                // MusicBrainzClient에서 이미 name이나 aliases 일치 확인을 했으므로, 결과가 있으면 사용
                if (mbInfo.getMbid() != null && !mbInfo.getMbid().isBlank()) {
                    String mbid = mbInfo.getMbid();
                    artist.setMusicBrainzId(mbid);
                    
                    // MusicBrainz ID로 아티스트 조회하여 relations에서 member of band의 artist.name 가져오기
                    Optional<MusicBrainzClient.ArtistInfo> mbDetailOpt = musicBrainzClient.getArtistByMbid(mbid);
                    if (mbDetailOpt.isPresent()) {
                        MusicBrainzClient.ArtistInfo mbDetail = mbDetailOpt.get();
                        
                        // relations 배열에서 member of band의 artist.name 가져오기
                        if (mbDetail.getArtistGroup() != null && !mbDetail.getArtistGroup().isBlank()) {
                            artist.setArtistGroup(mbDetail.getArtistGroup());
                            log.debug("소속 그룹 추출 성공 (MusicBrainz ID 조회): artistId={}, mbid={}, group={}",
                                    artist.getId(), mbid, mbDetail.getArtistGroup());
                        }
                    }
                    
                    artistRepository.save(artist);
                    log.info("✅ MusicBrainz ID 수집 성공: artistId={}, searchName={}, mbid={}, mbName={}, group={}",
                            artist.getId(), searchName, mbid, mbInfo.getName(), artist.getArtistGroup());
                } else {
                    log.warn("⚠️ MusicBrainz MBID가 비어있음: artistId={}, searchName={}", 
                            artist.getId(), searchName);
                }
            } else {
                log.warn("⚠️ MusicBrainz 검색 결과 없음: artistId={}, searchName={}",
                        artist.getId(), searchName);
            }
        } catch (Exception e) {
            log.warn("MusicBrainz ID 수집 실패: artistId={}, searchName={}, error={}",
                    artist.getId(), searchName, e.getMessage(), e);
        }
    }

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
        String artistTypeStr = result.artistType != null ? result.artistType : 
                (artist.getArtistType() != null ? artist.getArtistType().name() : null);
        
        // String을 ArtistType enum으로 변환
        ArtistType artistType;
        if (artistTypeStr != null) {
            try {
                artistType = ArtistType.valueOf(artistTypeStr);
            } catch (IllegalArgumentException e) {
                log.warn("잘못된 artistType 값: {}, null 사용", artistTypeStr);
                artistType = null;
            }
        } else {
            // 기존 값이 없고 새 값도 없으면 null 사용
            artistType = artist.getArtistType();
        }

        // artistGroup 처리: enrich에서 찾지 못했으면 기존 값 유지
        String finalArtistGroup = result.artistGroup != null ? result.artistGroup : artist.getArtistGroup();
        if (result.artistGroup == null && artist.getArtistGroup() != null) {
            log.debug("enrich에서 그룹 정보를 찾지 못했지만 기존 값 유지: artistId={}, 기존 group={}", 
                    artist.getId(), artist.getArtistGroup());
        }
        
        // artistType이 GROUP이면 artistGroup을 null로 설정 (그룹에는 그룹 이름이 아니라 null이어야 함)
        if (artistType == ArtistType.GROUP) {
            finalArtistGroup = null;
            log.debug("artistType이 GROUP이므로 artistGroup을 null로 설정: artistId={}, name={}", 
                    artist.getId(), artist.getArtistName());
        }
        
        // artistGroup 검증: 멤버 이름, 출연 프로그램, 소속사 등 잘못된 값 필터링
        if (finalArtistGroup != null) {
            finalArtistGroup = validateArtistGroup(finalArtistGroup, artist.getArtistName(), artist.getNameKo());
            if (finalArtistGroup == null) {
                log.debug("artistGroup 검증 실패로 null 처리: artistId={}, name={}", 
                        artist.getId(), artist.getArtistName());
            }
        }

        // 기존 row를 "보강"
        artist.updateProfile(result.nameKo, finalArtistGroup, artistType);
        // 명시적으로 save하여 변경사항을 DB에 즉시 반영
        artistRepository.save(artist);
        log.info("Enrich 성공: artistId={}, name={}, nameKo={}, group={}, type={}, source={}",
                artist.getId(), artist.getArtistName(), result.nameKo,
                finalArtistGroup, artistType, result.source);
    }

    private EnrichResult enrichArtist(Artist artist) {
        String nameKo = null;
        String artistGroup = null;
        String artistType = null;
        String source = "";

        // -1단계: MusicBrainz ID가 이미 있는 경우, 그것으로 직접 Wikidata 검색 (최우선)
        String mbidSource = null; // MBID 출처 추적: "direct-mbid", "wikidata" 또는 "spotify-url"
        JsonNode mbidWikidataEntity = null; // MusicBrainz ID로 찾은 Wikidata 엔티티
        String mbidWikidataQid = null;
        
        if (artist.getMusicBrainzId() != null && !artist.getMusicBrainzId().isBlank()) {
            try {
                Optional<String> qidOpt = wikidataClient.searchWikidataIdByMusicBrainzId(artist.getMusicBrainzId());
                if (qidOpt.isPresent()) {
                    mbidWikidataQid = qidOpt.get();
                    mbidSource = "direct-mbid";
                    log.info("MusicBrainz ID로 직접 Wikidata QID 검색 결과: mbid={}, qid={}, artistName={}", 
                            artist.getMusicBrainzId(), mbidWikidataQid, artist.getArtistName());
                    
                    Optional<JsonNode> entityOpt = wikidataClient.getEntityInfo(mbidWikidataQid);
                    if (entityOpt.isPresent()) {
                        mbidWikidataEntity = entityOpt.get();
                        
                        // 1. Wikidata P31로 artistType 판별
                        if (artistType == null) {
                            List<String> instanceOfList = wikidataClient.getAllEntityIdClaims(mbidWikidataEntity, "P31");
                            boolean isGroup = instanceOfList.contains("http://www.wikidata.org/entity/Q215380");
                            boolean isHuman = instanceOfList.contains("http://www.wikidata.org/entity/Q5");
                            
                            if (isGroup) {
                                artistType = "GROUP";
                                source += "Wikidata(MBID) ";
                                log.debug("artistType 추출 성공 (Wikidata via MBID): mbid={}, type=GROUP", 
                                        artist.getMusicBrainzId());
                            } else if (isHuman) {
                                artistType = "SOLO";
                                source += "Wikidata(MBID) ";
                                log.debug("artistType 추출 성공 (Wikidata via MBID): mbid={}, type=SOLO", 
                                        artist.getMusicBrainzId());
                            }
                        }
                        
                        // 2. MusicBrainz 상세 조회
                        Optional<MusicBrainzClient.ArtistInfo> mbInfoOpt = musicBrainzClient.getArtistByMbid(artist.getMusicBrainzId());
                        if (mbInfoOpt.isPresent()) {
                            MusicBrainzClient.ArtistInfo mbInfo = mbInfoOpt.get();
                            
                            // Type 덮어쓰기 정책: 합의(consensus) 방식
                            if (mbInfo.getArtistType() != null && !mbInfo.getArtistType().isBlank()) {
                                String mbType = mbInfo.getArtistType();
                                String wdType = artistType;
                                
                                if (wdType != null && wdType.equals(mbType)) {
                                    // 합의: 두 소스가 같으면 확정(High confidence)
                                    artistType = mbType;
                                    source = source.replace("Wikidata(MBID) ", "");
                                    source += "MusicBrainz(High) ";
                                    log.debug("artistType 합의 확정 (Wikidata=MusicBrainz via MBID): mbid={}, type={}", 
                                            artist.getMusicBrainzId(), mbType);
                                } else if (wdType == null) {
                                    // Wikidata에서 type을 못 찾았으면 MusicBrainz 사용
                                    artistType = mbType;
                                    source += "MusicBrainz ";
                                    log.debug("artistType 추출 성공 (MusicBrainz, Wikidata type 없음): mbid={}, type={}", 
                                            artist.getMusicBrainzId(), mbType);
                                } else {
                                    // 충돌: 덮어쓰기 금지, Wikidata 유지
                                    log.warn("artistType 충돌 감지 - 덮어쓰기 금지: mbid={}, Wikidata={}, MusicBrainz={}, Wikidata 유지", 
                                            artist.getMusicBrainzId(), wdType, mbType);
                                }
                            }
                            
                            // SOLO일 때만 group 추출 (Wikidata 우선, 없으면 MusicBrainz)
                            if ("SOLO".equals(artistType)) {
                                // Wikidata에서 먼저 시도
                                if (artistGroup == null) {
                                    artistGroup = resolveGroupNameFromWikidata(mbidWikidataEntity);
                                    if (artistGroup != null) {
                                        source += "Wikidata(MBID) ";
                                        log.debug("소속 그룹 추출 성공 (Wikidata via MBID): mbid={}, group={}", 
                                                artist.getMusicBrainzId(), artistGroup);
                                    }
                                }
                                // Wikidata에서 못 찾으면 MusicBrainz에서 시도
                                if (artistGroup == null && mbInfo.getArtistGroup() != null && 
                                    !mbInfo.getArtistGroup().isBlank()) {
                                    artistGroup = mbInfo.getArtistGroup();
                                    source += "MusicBrainz ";
                                    log.debug("소속 그룹 추출 성공 (MusicBrainz): mbid={}, group={}", 
                                            artist.getMusicBrainzId(), artistGroup);
                                }
                            }
                        }
                        
                        if (artistType != null || artistGroup != null) {
                            log.info("✅ -1단계 성공 (MBID 직접 검색): artistId={}, mbid={}, qid={}, type={}, group={}", 
                                    artist.getId(), artist.getMusicBrainzId(), mbidWikidataQid, artistType, artistGroup);
                        }
                    }
                } else {
                    log.debug("MusicBrainz ID로 Wikidata QID 검색 결과 없음: mbid={}", artist.getMusicBrainzId());
                }
            } catch (Exception e) {
                log.warn("⚠️ -1단계 실패 (MBID 직접 검색): artistId={}, mbid={} (예외 발생: {})", 
                        artist.getId(), artist.getMusicBrainzId(), e.getMessage(), e);
            }
        }

        // 0단계: Spotify ID 기반 (MusicBrainz ID로 찾지 못한 경우에만)
        if (mbidSource == null && artist.getSpotifyArtistId() != null && !artist.getSpotifyArtistId().isBlank()) {
            try {
                // Spotify ID로 Wikidata 후보 리스트 검색
                List<String> candidateQids = wikidataClient.searchWikidataIdCandidatesBySpotifyId(artist.getSpotifyArtistId());
                log.info("Spotify ID로 Wikidata QID 검색 결과: spotifyId={}, artistName={}, 후보 개수={}, QIDs={}", 
                        artist.getSpotifyArtistId(), artist.getArtistName(), candidateQids.size(), candidateQids);
                if (candidateQids.isEmpty()) {
                    log.debug("Spotify ID로 Wikidata 후보 없음: spotifyId={}", artist.getSpotifyArtistId());
                } else {
                    log.debug("Spotify ID로 Wikidata 후보 {}개 발견: spotifyId={}, candidates={}", 
                            candidateQids.size(), artist.getSpotifyArtistId(), candidateQids);
                }
                
                // 후보 리스트에서 검증하여 최적 QID 선택
                String qid = null;
                JsonNode entity = null;
                int bestScore = -1;
                
                for (String candidateQid : candidateQids) {
                    Optional<JsonNode> entityOpt = wikidataClient.getEntityInfo(candidateQid);
                    if (entityOpt.isEmpty()) {
                        continue;
                    }
                    
                    JsonNode candidateEntity = entityOpt.get();
                    int score = validateWikidataEntity(candidateEntity, artist.getArtistName(), nameKo);
                    
                    if (score > bestScore) {
                        bestScore = score;
                        qid = candidateQid;
                        entity = candidateEntity;
                    }
                }
                
                // 검증 통과한 QID가 있으면 사용
                if (qid != null && entity != null && bestScore > 0) {
                    log.debug("Wikidata QID 검증 통과: spotifyId={}, qid={}, score={}", 
                            artist.getSpotifyArtistId(), qid, bestScore);
                    
                    // 1. Wikidata P31로 artistType 판별
                    // musical group 있으면 GROUP, else human 있으면 SOLO
                    if (artistType == null) {
                        List<String> instanceOfList = wikidataClient.getAllEntityIdClaims(entity, "P31");
                        boolean isGroup = instanceOfList.contains("http://www.wikidata.org/entity/Q215380");
                        boolean isHuman = instanceOfList.contains("http://www.wikidata.org/entity/Q5");
                        
                        if (isGroup) {
                            artistType = "GROUP";
                            source += "Wikidata ";
                            log.debug("artistType 추출 성공 (Wikidata): spotifyId={}, type=GROUP", 
                                    artist.getSpotifyArtistId());
                        } else if (isHuman) {
                            artistType = "SOLO";
                            source += "Wikidata ";
                            log.debug("artistType 추출 성공 (Wikidata): spotifyId={}, type=SOLO", 
                                    artist.getSpotifyArtistId());
                        }
                    }
                    
                    // 2. Wikidata P434로 MBID 확보
                    List<String> mbids = wikidataClient.getAllEntityIdClaims(entity, "P434");
                    if (!mbids.isEmpty()) {
                        String mbid = mbids.get(0);
                        mbidSource = "wikidata";
                        log.debug("Wikidata에서 MusicBrainz ID 찾음: spotifyId={}, mbid={}", 
                                artist.getSpotifyArtistId(), mbid);
                        
                        // MBID 상세 조회
                        Optional<MusicBrainzClient.ArtistInfo> mbInfoOpt = musicBrainzClient.getArtistByMbid(mbid);
                        if (mbInfoOpt.isPresent()) {
                            MusicBrainzClient.ArtistInfo mbInfo = mbInfoOpt.get();
                            
                            // Type 덮어쓰기 정책: 합의(consensus) 방식
                            // Wikidata type과 MusicBrainz type이 같으면 → 확정(High)
                            // 서로 다르면 → 덮어쓰기 금지, Wikidata 유지 + confidence 낮춤
                            if (mbInfo.getArtistType() != null && !mbInfo.getArtistType().isBlank() && 
                                "wikidata".equals(mbidSource)) {
                                String mbType = mbInfo.getArtistType();
                                String wdType = artistType;
                                
                                if (wdType != null && wdType.equals(mbType)) {
                                    // 합의: 두 소스가 같으면 확정(High confidence)
                                    artistType = mbType;
                                    source = source.replace("Wikidata ", "");
                                    source += "MusicBrainz(High) ";
                                    log.debug("artistType 합의 확정 (Wikidata=MusicBrainz): spotifyId={}, mbid={}, type={}", 
                                            artist.getSpotifyArtistId(), mbid, mbType);
                                } else if (wdType == null) {
                                    // Wikidata에서 type을 못 찾았으면 MusicBrainz 사용
                                    artistType = mbType;
                                    source += "MusicBrainz ";
                                    log.debug("artistType 추출 성공 (MusicBrainz, Wikidata type 없음): spotifyId={}, mbid={}, type={}", 
                                            artist.getSpotifyArtistId(), mbid, mbType);
                                } else {
                                    // 충돌: 덮어쓰기 금지, Wikidata 유지
                                    log.warn("artistType 충돌 감지 - 덮어쓰기 금지: spotifyId={}, Wikidata={}, MusicBrainz={}, Wikidata 유지", 
                                            artist.getSpotifyArtistId(), wdType, mbType);
                                }
                            }
                            
                            // SOLO일 때만 group 추출 (Wikidata 우선, 없으면 MusicBrainz)
                            if ("SOLO".equals(artistType)) {
                                // Wikidata에서 먼저 시도 (SPARQL 쿼리 사용)
                                if (artistGroup == null) {
                                    List<String> groups = wikidataClient.searchGroupBySpotifyId(artist.getSpotifyArtistId());
                                    if (!groups.isEmpty()) {
                                        // 첫 번째 그룹 사용 (가장 대표적인 그룹)
                                        artistGroup = groups.get(0);
                                        source += "Wikidata(SPARQL) ";
                                        log.debug("소속 그룹 추출 성공 (Wikidata SPARQL): spotifyId={}, group={}, 후보={}", 
                                                artist.getSpotifyArtistId(), artistGroup, groups);
                                    } else {
                                        // SPARQL로 못 찾으면 기존 방식 시도
                                        artistGroup = resolveGroupNameFromWikidata(entity);
                                        if (artistGroup != null) {
                                            source += "Wikidata ";
                                            log.debug("소속 그룹 추출 성공 (Wikidata): spotifyId={}, group={}", 
                                                    artist.getSpotifyArtistId(), artistGroup);
                                        }
                                    }
                                }
                                // Wikidata에서 못 찾으면 MusicBrainz에서 시도
                                if (artistGroup == null && mbInfo.getArtistGroup() != null && 
                                    !mbInfo.getArtistGroup().isBlank()) {
                                    artistGroup = mbInfo.getArtistGroup();
                                    source += "MusicBrainz ";
                                    log.debug("소속 그룹 추출 성공 (MusicBrainz): spotifyId={}, mbid={}, group={}", 
                                            artist.getSpotifyArtistId(), mbid, artistGroup);
                                }
                            }
                        }
                    }
                    
                    if (artistType != null || artistGroup != null) {
                        log.info("✅ 0단계 성공: artistId={}, spotifyId={}, type={}, group={}", 
                                artist.getId(), artist.getSpotifyArtistId(), artistType, artistGroup);
                    }
                } else {
                    log.warn("⚠️ Wikidata QID 검증 실패: spotifyId={}, 후보 {}개 중 검증 통과 없음", 
                            artist.getSpotifyArtistId(), candidateQids.size());
                }
                
                // 0.5단계: MusicBrainz에서 Spotify URL로 MBID 검색 (Wikidata에서 못 찾은 경우)
                if (mbidSource == null) {
                    Optional<String> mbidFromSpotifyOpt = musicBrainzClient.searchMbidBySpotifyUrl(artist.getSpotifyArtistId());
                    if (mbidFromSpotifyOpt.isPresent()) {
                        String mbid = mbidFromSpotifyOpt.get();
                        mbidSource = "spotify-url";
                        log.debug("Spotify URL로 MusicBrainz ID 찾음: spotifyId={}, mbid={}", 
                                artist.getSpotifyArtistId(), mbid);
                        
                        // MBID 상세 조회
                        Optional<MusicBrainzClient.ArtistInfo> mbInfoOpt = musicBrainzClient.getArtistByMbid(mbid);
                        if (mbInfoOpt.isPresent()) {
                            MusicBrainzClient.ArtistInfo mbInfo = mbInfoOpt.get();
                            
                            // Type 덮어쓰기 정책: 합의(consensus) 방식
                            if (mbInfo.getArtistType() != null && !mbInfo.getArtistType().isBlank() && 
                                "spotify-url".equals(mbidSource)) {
                                String mbType = mbInfo.getArtistType();
                                String wdType = artistType;
                                
                                if (wdType != null && wdType.equals(mbType)) {
                                    // 합의: 두 소스가 같으면 확정(High confidence)
                                    artistType = mbType;
                                    source += "MusicBrainz(High) ";
                                    log.debug("artistType 합의 확정 (Wikidata=MusicBrainz): spotifyId={}, mbid={}, type={}", 
                                            artist.getSpotifyArtistId(), mbid, mbType);
                                } else if (wdType == null) {
                                    // Wikidata에서 type을 못 찾았으면 MusicBrainz 사용
                                    artistType = mbType;
                                    source += "MusicBrainz ";
                                    log.debug("artistType 추출 성공 (MusicBrainz, Wikidata type 없음): spotifyId={}, mbid={}, type={}", 
                                            artist.getSpotifyArtistId(), mbid, mbType);
                                } else {
                                    // 충돌: 덮어쓰기 금지, Wikidata 유지
                                    log.warn("artistType 충돌 감지 - 덮어쓰기 금지: spotifyId={}, Wikidata={}, MusicBrainz={}, Wikidata 유지", 
                                            artist.getSpotifyArtistId(), wdType, mbType);
                                }
                            }
                            
                            // SOLO일 때만 group 추출 (MusicBrainz만 사용, Wikidata는 이미 시도했거나 없음)
                            if ("SOLO".equals(artistType) && artistGroup == null) {
                                if (mbInfo.getArtistGroup() != null && !mbInfo.getArtistGroup().isBlank()) {
                                    artistGroup = mbInfo.getArtistGroup();
                                    source += "MusicBrainz ";
                                    log.debug("소속 그룹 추출 성공 (MusicBrainz): spotifyId={}, mbid={}, group={}", 
                                            artist.getSpotifyArtistId(), mbid, artistGroup);
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("⚠️ 0단계 실패: artistId={}, spotifyId={} (예외 발생: {})", 
                        artist.getId(), artist.getSpotifyArtistId(), e.getMessage(), e);
            }
        }

        // 1단계: FLO Client로 한국어 이름 가져오기
        try {
            Optional<FloClient.ArtistInfo> floInfoOpt = floClient.searchArtist(artist.getArtistName());
            if (floInfoOpt.isPresent()) {
                FloClient.ArtistInfo floInfo = floInfoOpt.get();
                
                // 한국어 이름만 가져오기
                if (floInfo.getNameKo() != null && !floInfo.getNameKo().isBlank()) {
                    nameKo = floInfo.getNameKo();
                    source += "FLO ";
                    log.info("✅ 1단계 성공: artistId={}, name={}, nameKo={}", 
                            artist.getId(), artist.getArtistName(), nameKo);
                } else {
                    log.warn("⚠️ 1단계 실패: artistId={}, name={} (FLO에서 한국어 이름 없음)", 
                            artist.getId(), artist.getArtistName());
                }
            } else {
                log.warn("⚠️ 1단계 실패: artistId={}, name={} (FLO 검색 결과 없음)", 
                        artist.getId(), artist.getArtistName());
            }
        } catch (Exception e) {
            log.warn("⚠️ 1단계 실패: artistId={}, name={} (예외 발생: {})", 
                    artist.getId(), artist.getArtistName(), e.getMessage());
        }

        // nameKo는 optional - 없어도 다음 단계 진행 가능

        // 2단계: nameKo 기반 MB 검색 (0단계에서 MBID 못 찾았을 때만)
        // artistType만 보조적으로 사용, 기존 값 없을 때만 채움
        // artist.type이 Group/Person이 명확한 것만 사용
        // artistGroup은 절대 설정하지 않음
        if (mbidSource == null && artistType == null) {
            // nameKo가 없으면 검색 불가
            if (nameKo == null || nameKo.isBlank()) {
                log.debug("2단계 스킵: nameKo가 없어서 MusicBrainz 검색 불가");
            } else {
                try {
                    Optional<MusicBrainzClient.ArtistInfo> mbInfoOpt = musicBrainzClient.searchArtist(nameKo);
                    if (mbInfoOpt.isPresent()) {
                        MusicBrainzClient.ArtistInfo mbInfo = mbInfoOpt.get();

                        // artistType만 보조적으로 사용 (기존 값 없을 때만 채움)
                        // artist.type이 Group/Person이 명확한 것만 사용
                        // Person → SOLO, Group → GROUP
                        // 중요: "소속 그룹이 있다"는 이유로 SOLO를 GROUP으로 바꾸면 안 됨
                        if (artistType == null && mbInfo.getArtistType() != null && !mbInfo.getArtistType().isBlank()) {
                            String mbType = mbInfo.getArtistType();
                            // Group 또는 Person이 명확한 경우만 사용
                            if ("GROUP".equals(mbType) || "SOLO".equals(mbType)) {
                                artistType = mbType;
                                source += "MusicBrainz(LOW) ";
                                log.debug("artistType 추출 성공 (MusicBrainz, LOW confidence): nameKo={}, type={}", nameKo, artistType);
                            } else {
                                log.debug("artistType이 명확하지 않음, 무시: nameKo={}, type={}", nameKo, mbType);
                            }
                        }

                        // artistGroup은 절대 설정하지 않음 (2단계에서는 group 추출 금지)
                        if (artistType != null) {
                            log.info("✅ 2단계 성공: artistId={}, name={}, nameKo={}, type={}", 
                                    artist.getId(), artist.getArtistName(), nameKo, artistType);
                        }
                    } else {
                        log.warn("⚠️ 2단계 실패: artistId={}, name={}, nameKo={} (검색 결과 없음)", 
                                artist.getId(), artist.getArtistName(), nameKo);
                    }
                } catch (Exception e) {
                    log.warn("⚠️ 2단계 실패: artistId={}, name={}, nameKo={} (예외 발생: {})",
                            artist.getId(), artist.getArtistName(), nameKo, e.getMessage(), e);
                }
            }
        }

        return new EnrichResult(nameKo, artistGroup, artistType, source.trim());
    }

    // Wikidata 엔티티에서 정보 추출 (한국이름-활동명, 소속그룹, 솔로/그룹)
    private EnrichResult extractInfoFromWikidata(JsonNode entity) {
        String nameKo = null;
        String artistGroup = null;
        String artistType = null;

        // 1. 한국어 이름 가져오기 (활동명 기준)
        // 우선순위: labels.ko.value (활동명) > Wikipedia 한국어 이름
        JsonNode koLabel = entity.path("labels").path("ko").path("value");
        if (!koLabel.isMissingNode() && !koLabel.asText().isBlank()) {
            nameKo = koLabel.asText();
            log.debug("Wikidata 한국어 label (활동명) 확보: {}", nameKo);
        } else {
            // Wikipedia에서 한국어 이름 가져오기
            Optional<String> nameKoOpt = wikidataClient.getKoreanNameFromWikipedia(entity);
            if (nameKoOpt.isPresent()) {
                nameKo = nameKoOpt.get();
                log.debug("Wikidata Wikipedia 한국어 이름 확보: {}", nameKo);
            }
        }

        // 2. 아티스트 타입 추출 (솔로/그룹)
        artistType = inferArtistTypeFromWikidata(entity);

        // 3. 소속 그룹 추출
        artistGroup = resolveGroupNameFromWikidata(entity);

        // 한국어 이름이 있어야만 성공으로 간주
        if (nameKo == null) {
            return null;
        }

        return new EnrichResult(nameKo, artistGroup, artistType, "");
    }

    //Wikidata 엔티티에서 아티스트 타입 추출
    private String inferArtistTypeFromWikidata(JsonNode entity) {
        // P31 instance of: human(Q5), musical group(Q215380)
        List<String> instanceOfList = wikidataClient.getAllEntityIdClaims(entity, "P31");
        log.debug("Wikidata instanceOf 목록: {}", instanceOfList);

        // 타입 판별 로직
        boolean isGroup = instanceOfList.contains("http://www.wikidata.org/entity/Q215380");
        boolean isHuman = instanceOfList.contains("http://www.wikidata.org/entity/Q5");

        if (isGroup) {
            log.debug("Q215380 (musical group) 발견 -> GROUP");
            return "GROUP";
        } else if (isHuman) {
            log.debug("Q5 (human) 발견 -> SOLO");
            return "SOLO";
        }

        log.warn("Wikidata에서 artistType을 판단할 수 없음: instanceOf={}", instanceOfList);
        return null;
    }

    // Wikidata 엔티티에서 소속 그룹 이름 추출 (음악 그룹만, 출연 프로그램/소속사 제외)
    // 대표 그룹 선택 규칙: 한국 그룹 우선, nameKo/label 우선, 정렬 기준 적용
    private String resolveGroupNameFromWikidata(JsonNode artistEntity) {
        log.debug("소속 그룹 추출 시작: Wikidata 엔티티에서 P463 (member of) 속성 확인");
        
        // 모든 member of 엔티티 가져오기
        List<String> memberOfQids = wikidataClient.getAllEntityIdClaims(artistEntity, "P463");
        if (memberOfQids.isEmpty()) {
            log.debug("Wikidata에서 member of (P463) 속성 없음 - 소속 그룹 없음");
            return null;
        }

        log.debug("Wikidata에서 member of 엔티티 {}개 발견: {}", memberOfQids.size(), memberOfQids);

        // 음악 그룹 후보 수집 (P31 = Q215380 필터 필수)
        List<GroupCandidate> candidates = new java.util.ArrayList<>();
        
        for (String qid : memberOfQids) {
            log.debug("그룹 엔티티 확인 중: qid={}", qid);
            Optional<JsonNode> entityOpt = wikidataClient.getEntityInfo(qid);
            if (entityOpt.isEmpty()) {
                log.warn("Wikidata 엔티티 조회 실패: qid={}, 다음 후보 확인", qid);
                continue;
            }

            JsonNode entity = entityOpt.get();
            
            // 음악 그룹인지 확인 (P31 = Q215380) - 필터 필수
            List<String> instanceOfList = wikidataClient.getAllEntityIdClaims(entity, "P31");
            log.debug("엔티티 instanceOf 확인: qid={}, instanceOf={}", qid, instanceOfList);
            
            boolean isGroup = instanceOfList.contains("http://www.wikidata.org/entity/Q215380");
            if (!isGroup) {
                // 음악 그룹이 아니면 스킵 (출연 프로그램, 소속사 등 제외)
                log.debug("음악 그룹이 아님, 제외: qid={}, instanceOf={} (출연 프로그램/소속사일 가능성)", qid, instanceOfList);
                continue;
            }

            // 음악 그룹 확인됨 - 후보에 추가
            log.debug("음악 그룹 확인됨: qid={}", qid);
            
            // 그룹 이름 추출 시도 (우선순위 순)
            String koLabel = null;
            String nameKo = null;
            String enLabel = null;
            String wikiTitle = null;
            
            // 1. 한국어 label
            JsonNode koLabelNode = entity.path("labels").path("ko").path("value");
            if (!koLabelNode.isMissingNode() && !koLabelNode.asText().isBlank()) {
                koLabel = koLabelNode.asText();
            }
            
            // 2. Wikipedia 한국어 이름
            Optional<String> nameKoOpt = wikidataClient.getKoreanNameFromWikipedia(entity);
            if (nameKoOpt.isPresent()) {
                nameKo = nameKoOpt.get();
            }
            
            // 3. 영어 label
            JsonNode enLabelNode = entity.path("labels").path("en").path("value");
            if (!enLabelNode.isMissingNode() && !enLabelNode.asText().isBlank()) {
                enLabel = enLabelNode.asText();
            }
            
            // 4. 한국어 Wikipedia 제목
            Optional<String> wikiTitleOpt = wikidataClient.getWikipediaKoreanTitle(entity);
            if (wikiTitleOpt.isPresent()) {
                wikiTitle = wikiTitleOpt.get();
            }
            
            // 대표 그룹 선택을 위한 점수 계산
            int score = 0;
            String selectedName = null;
            
            // 한국어 이름이 있으면 높은 점수
            if (koLabel != null) {
                score += 100;
                selectedName = koLabel;
            } else if (nameKo != null) {
                score += 90;
                selectedName = nameKo;
            } else if (wikiTitle != null) {
                score += 80;
                selectedName = wikiTitle;
            } else if (enLabel != null) {
                score += 50;
                selectedName = enLabel;
            }
            
            // P434 (MusicBrainz ID) 또는 공식 사이트/위키백과 링크가 있으면 보너스
            List<String> mbids = wikidataClient.getAllEntityIdClaims(entity, "P434");
            if (!mbids.isEmpty()) {
                score += 20;
            }
            
            // sitelinks가 있으면 보너스
            JsonNode sitelinks = entity.path("sitelinks");
            if (!sitelinks.isMissingNode() && sitelinks.size() > 0) {
                score += 10;
            }
            
            if (selectedName != null) {
                candidates.add(new GroupCandidate(qid, selectedName, score));
            }
        }

        if (candidates.isEmpty()) {
            log.warn("Wikidata에서 음악 그룹을 찾을 수 없음 (출연 프로그램, 소속사 등만 있음)");
            return null;
        }

        // 점수 순으로 정렬 (높은 점수 우선)
        candidates.sort((a, b) -> Integer.compare(b.score, a.score));
        
        // 최고 점수 후보 반환
        GroupCandidate best = candidates.get(0);
        log.debug("대표 그룹 선택: qid={}, name={}, score={} (후보 {}개 중)", 
                best.qid, best.name, best.score, candidates.size());
        return best.name;
    }
    
    // 그룹 후보 클래스
    private static class GroupCandidate {
        final String qid;
        final String name;
        final int score;
        
        GroupCandidate(String qid, String name, int score) {
            this.qid = qid;
            this.name = name;
            this.score = score;
        }
    }
    
    // Wikidata 엔티티 검증 (QID 후보 검증)
    private int validateWikidataEntity(JsonNode entity, String artistName, String nameKo) {
        int score = 0;
        
        // 1. P31(instance of)이 human(Q5) 또는 musical group(Q215380) 포함해야 함
        List<String> instanceOfList = wikidataClient.getAllEntityIdClaims(entity, "P31");
        boolean isGroup = instanceOfList.contains("http://www.wikidata.org/entity/Q215380");
        boolean isHuman = instanceOfList.contains("http://www.wikidata.org/entity/Q5");
        boolean hasValidType = isGroup || isHuman;
        
        if (!hasValidType) {
            log.debug("Wikidata 엔티티 검증 실패: P31에 human/musical group 없음, instanceOf={}", instanceOfList);
            return 0; // 검증 실패
        }
        score += 50; // 기본 점수
        
        // 2. label(ko/en)이 artistName 또는 nameKo와 부분일치
        String koLabel = entity.path("labels").path("ko").path("value").asText(null);
        String enLabel = entity.path("labels").path("en").path("value").asText(null);
        
        boolean nameMatches = false;
        if (artistName != null && !artistName.isBlank()) {
            String artistNameLower = artistName.toLowerCase().trim();
            if (koLabel != null && koLabel.toLowerCase().contains(artistNameLower)) {
                nameMatches = true;
                score += 30;
            }
            if (enLabel != null && enLabel.toLowerCase().contains(artistNameLower)) {
                nameMatches = true;
                score += 30;
            }
        }
        
        if (nameKo != null && !nameKo.isBlank()) {
            String nameKoLower = nameKo.toLowerCase().trim();
            if (koLabel != null && koLabel.toLowerCase().contains(nameKoLower)) {
                nameMatches = true;
                score += 30;
            }
        }
        
        if (!nameMatches) {
            log.debug("Wikidata 엔티티 검증 실패: label이 artistName/nameKo와 일치하지 않음, koLabel={}, enLabel={}, artistName={}, nameKo={}", 
                    koLabel, enLabel, artistName, nameKo);
            // 이름 일치가 없어도 기본 점수는 유지 (P31 검증 통과)
        }
        
        // 3. P434(MusicBrainz ID) 또는 공식 사이트/위키백과 링크가 있으면 우선
        List<String> mbids = wikidataClient.getAllEntityIdClaims(entity, "P434");
        if (!mbids.isEmpty()) {
            score += 20;
        }
        
        // sitelinks 확인 (위키백과 링크)
        JsonNode sitelinks = entity.path("sitelinks");
        if (!sitelinks.isMissingNode() && sitelinks.size() > 0) {
            score += 10;
        }
        
        return score;
    }
    
    // artistGroup 검증: 멤버 이름, 출연 프로그램, 소속사 등 잘못된 값 필터링
    private String validateArtistGroup(String groupName, String artistName, String nameKo) {
        if (groupName == null || groupName.isBlank()) {
            return null;
        }
        
        String normalizedGroupName = normalizeForComparison(groupName);
        String lowerGroupName = groupName.toLowerCase().trim();
        
        // 0. 그룹 이름이 너무 짧으면 의심 (개인 이름일 가능성)
        if (normalizedGroupName.length() <= 3) {
            log.debug("artistGroup이 너무 짧아서 null 처리: group={}, length={}", 
                    groupName, normalizedGroupName.length());
            return null;
        }
        
        // 1. 그룹 이름이 아티스트 이름과 동일하거나 유사한 경우 (멤버 이름인 경우)
        // 더 엄격한 검증: 정규화된 이름이 완전히 일치하거나, 한쪽이 다른 쪽을 포함하는 경우
        if (artistName != null && !artistName.isBlank()) {
            String normalizedArtistName = normalizeForComparison(artistName);
            String lowerArtistName = artistName.toLowerCase().trim();
            
            // 완전 일치
            if (normalizedGroupName.equals(normalizedArtistName)) {
                log.warn("artistGroup이 아티스트 이름과 완전 일치하여 null 처리: group={}, artistName={}", 
                        groupName, artistName);
                return null;
            }
            
            // 그룹 이름이 아티스트 이름을 포함하는 경우 (예: "RM"이 "RM of BTS"에 포함)
            // 하지만 반대는 허용 (예: "BTS"에 "RM"이 포함되는 것은 정상)
            if (normalizedGroupName.contains(normalizedArtistName) && 
                normalizedArtistName.length() >= 2) { // 너무 짧은 부분 문자열은 무시
                // 예외: 그룹 이름이 아티스트 이름으로 시작하거나 끝나는 경우만 필터링
                // (예: "RM"이 "RM"으로 시작하는 경우는 제외, "RM of BTS" 같은 경우는 허용)
                if (normalizedGroupName.startsWith(normalizedArtistName) || 
                    normalizedGroupName.endsWith(normalizedArtistName)) {
                    log.warn("artistGroup이 아티스트 이름으로 시작/끝나서 null 처리: group={}, artistName={}", 
                            groupName, artistName);
                    return null;
                }
            }
            
            // 아티스트 이름이 그룹 이름을 포함하는 경우 (예: "RM"에 "BTS"가 포함되는 것은 이상함)
            if (normalizedArtistName.contains(normalizedGroupName) && 
                normalizedGroupName.length() >= 2) {
                log.warn("artistGroup이 아티스트 이름에 포함되어 null 처리: group={}, artistName={}", 
                        groupName, artistName);
                return null;
            }
            
            // 대소문자 무시 완전 일치
            if (lowerGroupName.equals(lowerArtistName)) {
                log.warn("artistGroup이 아티스트 이름과 대소문자 무시 일치하여 null 처리: group={}, artistName={}", 
                        groupName, artistName);
                return null;
            }
        }
        
        if (nameKo != null && !nameKo.isBlank()) {
            String normalizedNameKo = normalizeForComparison(nameKo);
            String lowerNameKo = nameKo.toLowerCase().trim();
            
            // 완전 일치
            if (normalizedGroupName.equals(normalizedNameKo)) {
                log.warn("artistGroup이 nameKo와 완전 일치하여 null 처리: group={}, nameKo={}", 
                        groupName, nameKo);
                return null;
            }
            
            // 그룹 이름이 nameKo를 포함하는 경우
            if (normalizedGroupName.contains(normalizedNameKo) && 
                normalizedNameKo.length() >= 2) {
                if (normalizedGroupName.startsWith(normalizedNameKo) || 
                    normalizedGroupName.endsWith(normalizedNameKo)) {
                    log.warn("artistGroup이 nameKo로 시작/끝나서 null 처리: group={}, nameKo={}", 
                            groupName, nameKo);
                    return null;
                }
            }
            
            // nameKo가 그룹 이름을 포함하는 경우
            if (normalizedNameKo.contains(normalizedGroupName) && 
                normalizedGroupName.length() >= 2) {
                log.warn("artistGroup이 nameKo에 포함되어 null 처리: group={}, nameKo={}", 
                        groupName, nameKo);
                return null;
            }
            
            // 대소문자 무시 완전 일치
            if (lowerGroupName.equals(lowerNameKo)) {
                log.warn("artistGroup이 nameKo와 대소문자 무시 일치하여 null 처리: group={}, nameKo={}", 
                        groupName, nameKo);
                return null;
            }
        }
        
        // 2. 출연 프로그램 키워드 체크
        String[] programKeywords = {
            "produce", "show", "survival", "audition", "competition",
            "프로듀스", "쇼", "서바이벌", "오디션", "경쟁", "프로그램"
        };
        for (String keyword : programKeywords) {
            if (lowerGroupName.contains(keyword)) {
                log.debug("artistGroup이 출연 프로그램으로 판단되어 null 처리: group={}", groupName);
                return null;
            }
        }
        
        // 3. 소속사 키워드 체크 (주요 엔터테인먼트 회사)
        String[] companyKeywords = {
            "sm entertainment", "yg entertainment", "jyp entertainment", "cube entertainment",
            "pledis entertainment", "starship entertainment", "fantagio", "woollim",
            "fnc entertainment", "rbw", "source music", "bighit", "hybe",
            "sm", "yg", "jyp", "cube", "pledis", "starship", "fantagio",
            "woollim", "fnc", "source", "bighit", "hybe",
            "엔터테인먼트", "엔터", "기획사", "소속사"
        };
        for (String keyword : companyKeywords) {
            if (lowerGroupName.contains(keyword)) {
                log.debug("artistGroup이 소속사로 판단되어 null 처리: group={}", groupName);
                return null;
            }
        }
        
        return groupName;
    }
    
    // 이름 정규화 (비교용)
    private String normalizeForComparison(String name) {
        if (name == null) {
            return "";
        }
        return name.toLowerCase()
                .replaceAll("[\\s\\-_\\(\\)\\[\\]]", "") // 공백, 하이픈, 언더스코어, 괄호 제거
                .trim();
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
