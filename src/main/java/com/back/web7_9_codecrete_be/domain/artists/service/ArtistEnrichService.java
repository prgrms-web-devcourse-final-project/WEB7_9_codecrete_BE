package com.back.web7_9_codecrete_be.domain.artists.service;

import com.back.web7_9_codecrete_be.domain.artists.entity.Artist;
import com.back.web7_9_codecrete_be.domain.artists.entity.ArtistType;
import com.back.web7_9_codecrete_be.domain.artists.repository.ArtistRepository;
import com.back.web7_9_codecrete_be.global.musicbrainz.MusicBrainzClient;
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
    private final EnrichStepExecutor stepExecutor;
    private final ArtistGroupValidator groupValidator;

    // MusicBrainz ID만 받아오기
    public int fetchMusicBrainzIds(int limit) {
        int actualLimit = limit > 0 ? Math.min(limit, 300) : 100;
        List<Artist> targets = artistRepository.findByMusicBrainzIdIsNullOrderByIdAsc(
                PageRequest.of(0, actualLimit)
        );
        if (targets.isEmpty()) {
            return 0;
        }

        int updated = 0;
        for (Artist artist : targets) {
            try {
                fetchMusicBrainzId(artist);
                updated++;
                Thread.sleep(1100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                // 개별 실패는 로그 생략
            }
        }
        return updated;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void fetchMusicBrainzId(Artist artist) {
        // nameKo를 우선 사용, 없으면 artistName 사용
        String searchName = (artist.getNameKo() != null && !artist.getNameKo().isBlank()) 
                ? artist.getNameKo() 
                : artist.getArtistName();
        
        try {
            Optional<MusicBrainzClient.ArtistInfo> mbInfoOpt = musicBrainzClient.searchArtist(searchName);
            if (mbInfoOpt.isPresent() && mbInfoOpt.get().getMbid() != null && !mbInfoOpt.get().getMbid().isBlank()) {
                String mbid = mbInfoOpt.get().getMbid();
                artist.setMusicBrainzId(mbid);
                
                Optional<MusicBrainzClient.ArtistInfo> mbDetailOpt = musicBrainzClient.getArtistByMbid(mbid);
                if (mbDetailOpt.isPresent()) {
                    String artistGroup = mbDetailOpt.get().getArtistGroup();
                    if (artistGroup != null && !artistGroup.isBlank()) {
                        // 소속사, 출연 프로그램, 이벤트성 그룹 필터링
                        String validatedGroup = groupValidator.validate(artistGroup, artist.getArtistName(), artist.getNameKo());
                        if (validatedGroup != null) {
                            artist.setArtistGroup(validatedGroup);
                        }
                    }
                }
                
                artistRepository.save(artist);
            }
        } catch (Exception e) {
            // 개별 실패는 로그 생략
        }
    }

    // Wikidata + Wikipedia + MusicBrainz를 통합하여 아티스트 정보를 가져와 enrich를 수행
    public int enrichArtist(int limit) {
        int actualLimit = limit > 0 ? Math.min(limit, 300) : 100;
        List<Artist> targets = artistRepository.findByNameKoIsNullOrderByIdAsc(
                PageRequest.of(0, actualLimit)
        );
        if (targets.isEmpty()) {
            return 0;
        }
        
        int updated = 0;
        int failed = 0;
        for (Artist artist : targets) {
            try {
                enrichSingleArtist(artist);
                updated++;
                Thread.sleep(1100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                failed++;
            }
        }

        log.info("Enrich 완료: 성공={}, 실패={}, 총={}", updated, failed, targets.size());
        return updated;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void enrichSingleArtist(Artist artist) {
        EnrichResult result = enrichArtist(artist);

        if (result == null) {
            throw new RuntimeException("아티스트 정보를 찾을 수 없음");
        }

        String artistTypeStr = result.artistType != null ? result.artistType : 
                (artist.getArtistType() != null ? artist.getArtistType().name() : null);

        ArtistType artistType;
        if (artistTypeStr != null) {
            try {
                artistType = ArtistType.valueOf(artistTypeStr);
            } catch (IllegalArgumentException e) {
                artistType = null;
            }
        } else {
            artistType = artist.getArtistType();
        }

        String finalArtistGroup = result.artistGroup != null ? result.artistGroup : artist.getArtistGroup();
        
        if (artistType == ArtistType.GROUP) {
            finalArtistGroup = null;
        }
        
        if (finalArtistGroup != null) {
            finalArtistGroup = groupValidator.validate(finalArtistGroup, artist.getArtistName(), artist.getNameKo());
        }

        artist.updateProfile(result.nameKo, finalArtistGroup, artistType);
        artistRepository.save(artist);
    }

    private EnrichResult enrichArtist(Artist artist) {
        String nameKo = null;
        String artistGroup = null;
        String artistType = null;
        String source = "";

        // -1단계: MusicBrainz ID로 직접 Wikidata 검색
        EnrichStepExecutor.EnrichStepResult stepMinusOne = stepExecutor.executeStepMinusOne(artist, artistType, artistGroup);
        if (stepMinusOne.artistType != null || stepMinusOne.artistGroup != null) {
            artistType = stepMinusOne.artistType != null ? stepMinusOne.artistType : artistType;
            artistGroup = stepMinusOne.artistGroup != null ? stepMinusOne.artistGroup : artistGroup;
            source += stepMinusOne.source != null ? stepMinusOne.source : "";
        }

        // 0단계: Spotify ID 기반 검색
        if (artist.getMusicBrainzId() == null || artist.getMusicBrainzId().isBlank()) {
            EnrichStepExecutor.EnrichStepResult stepZero = stepExecutor.executeStepZero(artist, artistType, artistGroup, nameKo);
            if (stepZero.artistType != null || stepZero.artistGroup != null) {
                artistType = stepZero.artistType != null ? stepZero.artistType : artistType;
                artistGroup = stepZero.artistGroup != null ? stepZero.artistGroup : artistGroup;
                source += stepZero.source != null ? stepZero.source : "";
            }
        }

        // 1단계: FLO Client로 한국어 이름 가져오기
        EnrichStepExecutor.EnrichStepResult stepOne = stepExecutor.executeStepOne(artist);
        if (stepOne.nameKo != null) {
            nameKo = stepOne.nameKo;
            source += stepOne.source != null ? stepOne.source : "";
        }

        // 2단계: nameKo 기반 MusicBrainz 검색
        EnrichStepExecutor.EnrichStepResult stepTwo = stepExecutor.executeStepTwo(nameKo, artistType);
        if (stepTwo.artistType != null) {
            artistType = stepTwo.artistType;
            source += stepTwo.source != null ? stepTwo.source : "";
        }

        return new EnrichResult(nameKo, artistGroup, artistType, source.trim());
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
