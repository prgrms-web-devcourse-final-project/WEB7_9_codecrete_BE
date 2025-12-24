package com.back.web7_9_codecrete_be.domain.artists.service;

import com.back.web7_9_codecrete_be.domain.artists.entity.Artist;
import com.back.web7_9_codecrete_be.global.flo.FloClient;
import com.back.web7_9_codecrete_be.global.musicbrainz.MusicBrainzClient;
import com.back.web7_9_codecrete_be.global.wikidata.WikidataClient;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class EnrichStepExecutor {

    private final FloClient floClient;
    private final MusicBrainzClient musicBrainzClient;
    private final WikidataClient wikidataClient;
    private final WikidataEnrichHelper wikidataHelper;

    /**
     * -1단계: MusicBrainz ID로 직접 Wikidata 검색
     */
    public EnrichStepResult executeStepMinusOne(Artist artist, String currentArtistType, String currentArtistGroup) {
        if (artist.getMusicBrainzId() == null || artist.getMusicBrainzId().isBlank()) {
            return new EnrichStepResult(null, null, null, null);
        }

        try {
            Optional<String> qidOpt = wikidataClient.searchWikidataIdByMusicBrainzId(artist.getMusicBrainzId());
            if (qidOpt.isEmpty()) {
                return new EnrichStepResult(null, null, null, null);
            }

            String qid = qidOpt.get();
            Optional<JsonNode> entityOpt = wikidataClient.getEntityInfo(qid);
            if (entityOpt.isEmpty()) {
                return new EnrichStepResult(null, null, null, null);
            }

            JsonNode mbidWikidataEntity = entityOpt.get();
            String artistType = currentArtistType;
            String source = "";

            if (artistType == null) {
                artistType = wikidataHelper.inferArtistType(mbidWikidataEntity);
                if (artistType != null) {
                    source += "Wikidata(MBID) ";
                }
            }

            Optional<MusicBrainzClient.ArtistInfo> mbInfoOpt = musicBrainzClient.getArtistByMbid(artist.getMusicBrainzId());
            if (mbInfoOpt.isPresent()) {
                MusicBrainzClient.ArtistInfo mbInfo = mbInfoOpt.get();
                artistType = resolveTypeConsensus(artistType, mbInfo.getArtistType(), source);
                source = artistType != null && artistType.equals(mbInfo.getArtistType()) 
                    ? source.replace("Wikidata(MBID) ", "") + "MusicBrainz(High) " 
                    : source;

                String artistGroup = currentArtistGroup;
                if ("SOLO".equals(artistType)) {
                    if (artistGroup == null) {
                        artistGroup = wikidataHelper.resolveGroupName(mbidWikidataEntity);
                        if (artistGroup != null) {
                            source += "Wikidata(MBID) ";
                        }
                    }
                    if (artistGroup == null && mbInfo.getArtistGroup() != null && !mbInfo.getArtistGroup().isBlank()) {
                        artistGroup = mbInfo.getArtistGroup();
                        source += "MusicBrainz ";
                    }
                }

                return new EnrichStepResult(null, artistGroup, artistType, source);
            }

            return new EnrichStepResult(null, currentArtistGroup, artistType, source);
        } catch (Exception e) {
            return new EnrichStepResult(null, null, null, null);
        }
    }

    /**
     * 0단계: Spotify ID 기반 검색
     */
    public EnrichStepResult executeStepZero(Artist artist, String currentArtistType, String currentArtistGroup, String currentNameKo) {
        if (artist.getSpotifyArtistId() == null || artist.getSpotifyArtistId().isBlank()) {
            return new EnrichStepResult(null, null, null, null);
        }

        try {
            List<String> candidateQids = wikidataClient.searchWikidataIdCandidatesBySpotifyId(artist.getSpotifyArtistId());
            
            String qid = null;
            JsonNode entity = null;
            int bestScore = -1;
            
            for (String candidateQid : candidateQids) {
                Optional<JsonNode> entityOpt = wikidataClient.getEntityInfo(candidateQid);
                if (entityOpt.isEmpty()) continue;
                
                int score = wikidataHelper.validateEntity(entityOpt.get(), artist.getArtistName(), currentNameKo);
                if (score > bestScore) {
                    bestScore = score;
                    qid = candidateQid;
                    entity = entityOpt.get();
                }
            }
            
            if (qid == null || entity == null || bestScore <= 0) {
                return executeStepZeroPointFive(artist, currentArtistType, currentArtistGroup);
            }

            String artistType = currentArtistType;
            String source = "";

            if (artistType == null) {
                artistType = wikidataHelper.inferArtistType(entity);
                if (artistType != null) {
                    source += "Wikidata ";
                }
            }

            List<String> mbids = wikidataClient.getAllEntityIdClaims(entity, "P434");
            if (!mbids.isEmpty()) {
                String mbid = mbids.get(0);
                Optional<MusicBrainzClient.ArtistInfo> mbInfoOpt = musicBrainzClient.getArtistByMbid(mbid);
                if (mbInfoOpt.isPresent()) {
                    MusicBrainzClient.ArtistInfo mbInfo = mbInfoOpt.get();
                    artistType = resolveTypeConsensus(artistType, mbInfo.getArtistType(), source);
                    source = artistType != null && artistType.equals(mbInfo.getArtistType()) 
                        ? source.replace("Wikidata ", "") + "MusicBrainz(High) " 
                        : source;

                    String artistGroup = currentArtistGroup;
                    if ("SOLO".equals(artistType)) {
                        if (artistGroup == null) {
                            List<String> groups = wikidataClient.searchGroupBySpotifyId(artist.getSpotifyArtistId());
                            if (!groups.isEmpty()) {
                                artistGroup = groups.get(0);
                                source += "Wikidata(SPARQL) ";
                            } else {
                                artistGroup = wikidataHelper.resolveGroupName(entity);
                                if (artistGroup != null) {
                                    source += "Wikidata ";
                                }
                            }
                        }
                        if (artistGroup == null && mbInfo.getArtistGroup() != null && !mbInfo.getArtistGroup().isBlank()) {
                            artistGroup = mbInfo.getArtistGroup();
                            source += "MusicBrainz ";
                        }
                    }

                    return new EnrichStepResult(null, artistGroup, artistType, source);
                }
            }

            return new EnrichStepResult(null, currentArtistGroup, artistType, source);
        } catch (Exception e) {
            return executeStepZeroPointFive(artist, currentArtistType, currentArtistGroup);
        }
    }

    /**
     * 0.5단계: Spotify URL로 MusicBrainz ID 검색
     */
    private EnrichStepResult executeStepZeroPointFive(Artist artist, String currentArtistType, String currentArtistGroup) {
        try {
            Optional<String> mbidOpt = musicBrainzClient.searchMbidBySpotifyUrl(artist.getSpotifyArtistId());
            if (mbidOpt.isEmpty()) {
                return new EnrichStepResult(null, null, null, null);
            }

            String mbid = mbidOpt.get();
            Optional<MusicBrainzClient.ArtistInfo> mbInfoOpt = musicBrainzClient.getArtistByMbid(mbid);
            if (mbInfoOpt.isEmpty()) {
                return new EnrichStepResult(null, null, null, null);
            }

            MusicBrainzClient.ArtistInfo mbInfo = mbInfoOpt.get();
            String artistType = resolveTypeConsensus(currentArtistType, mbInfo.getArtistType(), "");
            String source = artistType != null && artistType.equals(mbInfo.getArtistType()) 
                ? "MusicBrainz(High) " 
                : "MusicBrainz ";

            String artistGroup = currentArtistGroup;
            if ("SOLO".equals(artistType) && artistGroup == null) {
                if (mbInfo.getArtistGroup() != null && !mbInfo.getArtistGroup().isBlank()) {
                    artistGroup = mbInfo.getArtistGroup();
                    source += "MusicBrainz ";
                }
            }

            return new EnrichStepResult(null, artistGroup, artistType, source);
        } catch (Exception e) {
            return new EnrichStepResult(null, null, null, null);
        }
    }

    /**
     * 1단계: FLO Client로 한국어 이름 가져오기
     */
    public EnrichStepResult executeStepOne(Artist artist) {
        try {
            Optional<FloClient.ArtistInfo> floInfoOpt = floClient.searchArtist(artist.getArtistName());
            if (floInfoOpt.isPresent() && floInfoOpt.get().getNameKo() != null && !floInfoOpt.get().getNameKo().isBlank()) {
                return new EnrichStepResult(floInfoOpt.get().getNameKo(), null, null, "FLO ");
            }
        } catch (Exception e) {
            // 실패는 조용히 넘어감
        }
        return new EnrichStepResult(null, null, null, null);
    }

    /**
     * 2단계: nameKo 기반 MusicBrainz 검색
     */
    public EnrichStepResult executeStepTwo(String nameKo, String currentArtistType) {
        if (nameKo == null || nameKo.isBlank() || currentArtistType != null) {
            return new EnrichStepResult(null, null, null, null);
        }

        try {
            Optional<MusicBrainzClient.ArtistInfo> mbInfoOpt = musicBrainzClient.searchArtist(nameKo);
            if (mbInfoOpt.isPresent() && mbInfoOpt.get().getArtistType() != null && 
                !mbInfoOpt.get().getArtistType().isBlank()) {
                String mbType = mbInfoOpt.get().getArtistType();
                if ("GROUP".equals(mbType) || "SOLO".equals(mbType)) {
                    return new EnrichStepResult(null, null, mbType, "MusicBrainz(LOW) ");
                }
            }
        } catch (Exception e) {
            // 실패는 조용히 넘어감
        }
        return new EnrichStepResult(null, null, null, null);
    }

    /**
     * 타입 합의 로직 (Wikidata와 MusicBrainz 타입 비교)
     */
    private String resolveTypeConsensus(String wdType, String mbType, String currentSource) {
        if (mbType == null || mbType.isBlank()) {
            return wdType;
        }

        if (wdType != null && wdType.equals(mbType)) {
            return mbType; // 합의: 두 소스가 같으면 확정
        } else if (wdType == null) {
            return mbType; // Wikidata에서 못 찾았으면 MusicBrainz 사용
        } else {
            return wdType; // 충돌: Wikidata 유지
        }
    }

    /**
     * 단계 실행 결과
     */
    public static class EnrichStepResult {
        final String nameKo;
        final String artistGroup;
        final String artistType;
        final String source;

        EnrichStepResult(String nameKo, String artistGroup, String artistType, String source) {
            this.nameKo = nameKo;
            this.artistGroup = artistGroup;
            this.artistType = artistType;
            this.source = source;
        }
    }
}
