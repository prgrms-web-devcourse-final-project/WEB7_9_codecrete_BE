package com.back.web7_9_codecrete_be.domain.artists.service.artistEnrichService;

import com.back.web7_9_codecrete_be.domain.artists.entity.Artist;
import com.back.web7_9_codecrete_be.domain.artists.entity.ArtistType;
import com.back.web7_9_codecrete_be.domain.artists.repository.ArtistRepository;
import com.back.web7_9_codecrete_be.global.maniadb.ManiaDBClient;
import com.back.web7_9_codecrete_be.global.musicbrainz.MusicBrainzClient;
import com.back.web7_9_codecrete_be.global.wikidata.WikidataClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArtistEnrichService {

    private final ArtistRepository artistRepository;
    private final MusicBrainzClient musicBrainzClient;
    private final WikidataClient wikidataClient;
    private final ManiaDBClient maniaDBClient;
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
                        // 소속사, 출연 프로그램, 이벤트성 그룹 필터링 (MusicBrainz는 HIGH 신뢰도)
                        String validatedGroup = groupValidator.validate(artistGroup, artist.getArtistName(), artist.getNameKo(), 
                                ArtistGroupValidator.SourceTrustLevel.HIGH);
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
            // 소스 신뢰도 판단: source에 "FLO"가 포함되어 있으면 LOW, 그 외는 HIGH
            ArtistGroupValidator.SourceTrustLevel trustLevel = 
                    (result.source != null && result.source.contains("FLO")) 
                    ? ArtistGroupValidator.SourceTrustLevel.LOW 
                    : ArtistGroupValidator.SourceTrustLevel.HIGH;
            finalArtistGroup = groupValidator.validate(finalArtistGroup, artist.getArtistName(), artist.getNameKo(), trustLevel);
        }

        artist.updateProfile(result.nameKo, finalArtistGroup, artistType);
        
        // 실명 수집 (1순위: Wikidata, 2순위: MusicBrainz)
        fetchRealName(artist);
        
        // 설명 수집 (1순위: Wikidata ko, 2순위: Wikidata en, 3순위: MusicBrainz+Wikidata 메타)
        fetchDescription(artist);
        
        artistRepository.save(artist);
    }
    
    /**
     * 실명 수집 (순차적 우선순위 방식)
     * 
     * 우선순위:
     * 1. Wikidata (ko) - P1477 @ko 또는 P735+P734 @ko
     * 2. ManiaDB '본명' (ko 중심)
     * 3. MusicBrainz aliases
     * 4. Wikidata (en) - P1477 @en 또는 P735+P734 @en
     * 5. 실패 → null
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void fetchRealName(Artist artist) {
        // 이미 실명이 있으면 스킵 (재수집 방지)
        if (artist.getRealName() != null && !artist.getRealName().isBlank()) {
            return;
        }
        
        // Spotify ID가 없으면 수집 불가
        if (artist.getSpotifyArtistId() == null || artist.getSpotifyArtistId().isBlank()) {
            log.debug("Spotify ID가 없어 실명 수집 불가: artistId={}", artist.getId());
            return;
        }
        
        // 1순위: Wikidata (ko)
        try {
            Optional<String> wikidataKoOpt = getWikidataRealNameKo(artist.getSpotifyArtistId());
            if (wikidataKoOpt.isPresent()) {
                String realName = wikidataKoOpt.get();
                if (realName != null && !realName.isBlank()) {
                    artist.setRealName(realName);
                    log.info("Wikidata(ko)에서 실명 수집 성공: artistId={}, spotifyId={}, realName={}", 
                            artist.getId(), artist.getSpotifyArtistId(), realName);
                    return;
                }
            }
        } catch (Exception e) {
            log.warn("Wikidata(ko) 실명 수집 실패: artistId={}, spotifyId={}", 
                    artist.getId(), artist.getSpotifyArtistId(), e);
        }
        
        // 2순위: ManiaDB '본명' (ko 중심)
        try {
            Optional<String> maniaDBOpt = maniaDBClient.getRealName(
                    artist.getArtistName(), 
                    artist.getNameKo()
            );
            if (maniaDBOpt.isPresent()) {
                String realName = maniaDBOpt.get();
                if (realName != null && !realName.isBlank()) {
                    artist.setRealName(realName);
                    log.info("ManiaDB에서 본명 수집 성공: artistId={}, artistName={}, realName={}", 
                            artist.getId(), artist.getArtistName(), realName);
                    return;
                }
            }
        } catch (Exception e) {
            log.warn("ManiaDB 본명 수집 실패: artistId={}, artistName={}", 
                    artist.getId(), artist.getArtistName(), e);
        }
        
        // 3순위: MusicBrainz aliases
        try {
            if (artist.getMusicBrainzId() != null && !artist.getMusicBrainzId().isBlank()) {
                Optional<String> mbOpt = getMusicBrainzRealName(artist.getMusicBrainzId(), artist.getArtistName());
                if (mbOpt.isPresent()) {
                    String realName = mbOpt.get();
                    if (realName != null && !realName.isBlank()) {
                        artist.setRealName(realName);
                        log.info("MusicBrainz에서 실명 수집 성공: artistId={}, mbid={}, realName={}", 
                                artist.getId(), artist.getMusicBrainzId(), realName);
                        return;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("MusicBrainz 실명 수집 실패: artistId={}, mbid={}", 
                    artist.getId(), artist.getMusicBrainzId(), e);
        }
        
        // 4순위: Wikidata (en)
        try {
            Optional<String> wikidataEnOpt = getWikidataRealNameEn(artist.getSpotifyArtistId());
            if (wikidataEnOpt.isPresent()) {
                String realName = wikidataEnOpt.get();
                if (realName != null && !realName.isBlank()) {
                    artist.setRealName(realName);
                    log.info("Wikidata(en)에서 실명 수집 성공: artistId={}, spotifyId={}, realName={}", 
                            artist.getId(), artist.getSpotifyArtistId(), realName);
                    return;
                }
            }
        } catch (Exception e) {
            log.warn("Wikidata(en) 실명 수집 실패: artistId={}, spotifyId={}", 
                    artist.getId(), artist.getSpotifyArtistId(), e);
        }
        
        // 모든 소스 실패
        log.debug("실명 수집 실패 (모든 소스 실패): artistId={}, spotifyId={}", 
                artist.getId(), artist.getSpotifyArtistId());
    }
    
    /**
     * Description 수집 실패 원인
     */
    private enum DescriptionFailureReason {
        NO_SPOTIFY_ID("Spotify ID 없음"),
        NO_QID_CANDIDATES("P1902로 QID 후보 없음"),
        ENTITY_NOT_FOUND("Entity 조회 실패"),
        NO_DESCRIPTIONS_FIELD("descriptions 필드 없음"),
        NO_DESCRIPTION_VALUE("ko/en/기타 언어 description 없음"),
        LOW_CONFIDENCE_SCORE("신뢰도 점수 미달"),
        NAME_BASED_SEARCH_FAILED("이름 기반 2차 탐색 실패");
        
        private final String message;
        
        DescriptionFailureReason(String message) {
            this.message = message;
        }
        
        public String getMessage() {
            return message;
        }
    }
    
    /**
     * 설명 수집 (Wikidata description만 사용)
     * 
     * 여러 QID 후보를 스코어링하여 가장 적합한 description 선택
     * - QID 후보 검색: P1902만 확인 (넓게 수집)
     * - 스코어링: 신뢰도 중심 + 언어 보너스
     * - 실패 시: 이름 기반 2차 탐색 (선택적)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void fetchDescription(Artist artist) {
        // Spotify ID가 없으면 수집 불가
        if (artist.getSpotifyArtistId() == null || artist.getSpotifyArtistId().isBlank()) {
            logDescriptionFailure(artist, DescriptionFailureReason.NO_SPOTIFY_ID);
            return;
        }
        
        // 1차: Spotify ID 기반 탐색
        DescriptionResult result = getWikidataDescriptionWithScoring(artist.getSpotifyArtistId(), artist.getArtistName(), artist.getNameKo());
        if (result.success) {
            artist.setDescription(result.description);
            log.info("Wikidata에서 설명 수집 성공: artistId={}, spotifyId={}, qid={}, 언어={}, 점수={}, description={}", 
                    artist.getId(), artist.getSpotifyArtistId(), result.qid, result.language, result.score, result.description);
            return;
        }
        
        // 2차: 이름 기반 탐색 (P1902 실패 시에만)
        if (result.failureReason == DescriptionFailureReason.NO_QID_CANDIDATES) {
            String searchName = artist.getNameKo() != null && !artist.getNameKo().isBlank() 
                    ? artist.getNameKo() 
                    : artist.getArtistName();
            if (searchName != null && !searchName.isBlank()) {
                DescriptionResult nameResult = getWikidataDescriptionByName(searchName, artist.getSpotifyArtistId());
                if (nameResult.success && nameResult.score >= 50) { // 신뢰도 50 이상만 채택
                    artist.setDescription(nameResult.description);
                    log.info("Wikidata에서 설명 수집 성공 (이름 기반): artistId={}, spotifyId={}, qid={}, 언어={}, 점수={}, description={}", 
                            artist.getId(), artist.getSpotifyArtistId(), nameResult.qid, nameResult.language, nameResult.score, nameResult.description);
                    return;
                }
            }
        }
        
        // 모든 시도 실패
        logDescriptionFailure(artist, result.failureReason);
    }
    
    private void logDescriptionFailure(Artist artist, DescriptionFailureReason reason) {
        log.warn("Wikidata description 수집 실패: artistId={}, spotifyId={}, artistName={}, 원인={}", 
                artist.getId(), artist.getSpotifyArtistId(), artist.getArtistName(), reason.getMessage());
    }
    
    /**
     * Description 수집 결과
     */
    private static class DescriptionResult {
        final boolean success;
        final String description;
        final String qid;
        final String language;
        final int score;
        final DescriptionFailureReason failureReason;
        
        DescriptionResult(boolean success, String description, String qid, String language, int score, DescriptionFailureReason failureReason) {
            this.success = success;
            this.description = description;
            this.qid = qid;
            this.language = language;
            this.score = score;
            this.failureReason = failureReason;
        }
        
        static DescriptionResult success(String description, String qid, String language, int score) {
            return new DescriptionResult(true, description, qid, language, score, null);
        }
        
        static DescriptionResult failure(DescriptionFailureReason reason) {
            return new DescriptionResult(false, null, null, null, 0, reason);
        }
    }
    
    /**
     * Wikidata에서 설명 조회 (스코어링 기반, 신뢰도 중심)
     * 
     * 여러 QID 후보를 조회하여 description이 있는 엔티티를 스코어링하고,
     * 가장 적합한 description을 선택
     * 
     * 스코어링 기준 (신뢰도 중심 + 언어 보너스):
     * - P1902(Spotify ID) 일치: +100점 (기본 신뢰도)
     * - P31이 사람/음악가/음악 그룹: +50점
     * - sitelinks 존재 (특히 ko/en): +30점
     * - label이 아티스트 이름과 유사: +20점
     * - 한국어 description: +20점 (보너스)
     * - 영어 description: +10점 (보너스)
     * - 기타 언어 description: +5점 (보너스)
     * 
     * 최소 신뢰도: 100점 (P1902 일치 필수)
     */
    private DescriptionResult getWikidataDescriptionWithScoring(String spotifyId, String artistName, String nameKo) {
        try {
            // 여러 QID 후보 조회 (P1902만 확인, 넓게 수집)
            List<String> candidateQids = wikidataClient.searchWikidataIdsBySpotifyId(spotifyId);
            if (candidateQids.isEmpty()) {
                return DescriptionResult.failure(DescriptionFailureReason.NO_QID_CANDIDATES);
            }
            
            log.debug("Wikidata QID 후보 {}개 발견: spotifyId={}, qids={}", 
                    candidateQids.size(), spotifyId, candidateQids);
            
            // 각 후보에 대해 description 스코어링
            List<DescriptionCandidate> candidates = new ArrayList<>();
            for (String qid : candidateQids) {
                try {
                    Optional<com.fasterxml.jackson.databind.JsonNode> entityOpt = wikidataClient.getEntityInfo(qid);
                    if (entityOpt.isEmpty()) {
                        continue;
                    }
                    
                    com.fasterxml.jackson.databind.JsonNode entity = entityOpt.get();
                    
                    // Entity 구조 검증: descriptions 필드 확인
                    com.fasterxml.jackson.databind.JsonNode descriptions = entity.path("descriptions");
                    if (descriptions.isMissingNode()) {
                        continue;
                    }
                    
                    // 신뢰도 중심 스코어링
                    DescriptionCandidate candidate = scoreDescriptionWithConfidence(
                            entity, descriptions, qid, spotifyId, artistName, nameKo);
                    if (candidate != null && candidate.score >= 100) { // 최소 신뢰도 100점
                        candidates.add(candidate);
                    }
                } catch (Exception e) {
                    log.debug("QID {} 처리 중 예외 발생: spotifyId={}", qid, spotifyId, e);
                }
            }
            
            if (candidates.isEmpty()) {
                // 원인 분석: descriptions 필드가 있는지 확인
                boolean hasDescriptionsField = false;
                for (String qid : candidateQids) {
                    try {
                        Optional<com.fasterxml.jackson.databind.JsonNode> entityOpt = wikidataClient.getEntityInfo(qid);
                        if (entityOpt.isPresent()) {
                            com.fasterxml.jackson.databind.JsonNode entity = entityOpt.get();
                            if (!entity.path("descriptions").isMissingNode()) {
                                hasDescriptionsField = true;
                                break;
                            }
                        }
                    } catch (Exception e) {
                        // 무시
                    }
                }
                
                DescriptionFailureReason reason = hasDescriptionsField 
                        ? DescriptionFailureReason.NO_DESCRIPTION_VALUE 
                        : DescriptionFailureReason.NO_DESCRIPTIONS_FIELD;
                return DescriptionResult.failure(reason);
            }
            
            // 점수가 높은 순으로 정렬
            candidates.sort((a, b) -> Integer.compare(b.score, a.score));
            
            DescriptionCandidate best = candidates.get(0);
            return DescriptionResult.success(best.description, best.qid, best.language, best.score);
            
        } catch (Exception e) {
            log.error("Wikidata 설명 조회 실패: spotifyId={}", spotifyId, e);
            return DescriptionResult.failure(DescriptionFailureReason.ENTITY_NOT_FOUND);
        }
    }
    
    /**
     * 이름 기반 Wikidata 설명 조회 (2차 탐색)
     * 
     * P1902로 찾지 못한 경우, 이름으로 검색하여 높은 신뢰도 점수를 받은 경우만 채택
     */
    private DescriptionResult getWikidataDescriptionByName(String searchName, String spotifyId) {
        try {
            Optional<String> qidOpt = wikidataClient.searchWikidataId(searchName);
            if (qidOpt.isEmpty()) {
                return DescriptionResult.failure(DescriptionFailureReason.NAME_BASED_SEARCH_FAILED);
            }
            
            String qid = qidOpt.get();
            Optional<com.fasterxml.jackson.databind.JsonNode> entityOpt = wikidataClient.getEntityInfo(qid);
            if (entityOpt.isEmpty()) {
                return DescriptionResult.failure(DescriptionFailureReason.ENTITY_NOT_FOUND);
            }
            
            com.fasterxml.jackson.databind.JsonNode entity = entityOpt.get();
            com.fasterxml.jackson.databind.JsonNode descriptions = entity.path("descriptions");
            
            if (descriptions.isMissingNode()) {
                return DescriptionResult.failure(DescriptionFailureReason.NO_DESCRIPTIONS_FIELD);
            }
            
            // 이름 기반 검색은 신뢰도 점수를 더 엄격하게 평가
            DescriptionCandidate candidate = scoreDescriptionWithConfidence(
                    entity, descriptions, qid, spotifyId, searchName, null);
            
            if (candidate != null && candidate.score >= 50) { // 최소 신뢰도 50점
                return DescriptionResult.success(candidate.description, candidate.qid, candidate.language, candidate.score);
            }
            
            return DescriptionResult.failure(DescriptionFailureReason.LOW_CONFIDENCE_SCORE);
            
        } catch (Exception e) {
            log.debug("이름 기반 설명 조회 실패: searchName={}, spotifyId={}", searchName, spotifyId, e);
            return DescriptionResult.failure(DescriptionFailureReason.NAME_BASED_SEARCH_FAILED);
        }
    }
    
    /**
     * Description 후보 정보
     */
    private static class DescriptionCandidate {
        final String qid;
        final String description;
        final String language;
        final int score;
        
        DescriptionCandidate(String qid, String description, String language, int score) {
            this.qid = qid;
            this.description = description;
            this.language = language;
            this.score = score;
        }
    }
    
    /**
     * Entity의 description을 신뢰도 중심으로 스코어링
     * 
     * 스코어링 기준:
     * - P1902(Spotify ID) 일치: +100점 (기본 신뢰도, 필수)
     * - P31이 사람/음악가/음악 그룹: +50점
     * - sitelinks 존재 (특히 ko/en): +30점
     * - label이 아티스트 이름과 유사: +20점
     * - 한국어 description: +20점 (보너스)
     * - 영어 description: +10점 (보너스)
     * - 기타 언어 description: +5점 (보너스)
     * 
     * @return DescriptionCandidate 또는 null (description이 없거나 신뢰도 미달인 경우)
     */
    private DescriptionCandidate scoreDescriptionWithConfidence(
            com.fasterxml.jackson.databind.JsonNode entity,
            com.fasterxml.jackson.databind.JsonNode descriptions,
            String qid,
            String spotifyId,
            String artistName,
            String nameKo) {
        
        int score = 0;
        String selectedDescription = null;
        String selectedLanguage = null;
        
        // 1. P1902(Spotify ID) 일치 확인 (필수, +100점)
        List<String> p1902Claims = wikidataClient.getAllEntityIdClaims(entity, "P1902");
        boolean hasSpotifyId = false;
        for (String claim : p1902Claims) {
            // claim은 보통 "http://www.wikidata.org/entity/Q..." 형식이거나 단순 문자열일 수 있음
            if (claim.contains(spotifyId) || claim.equals(spotifyId)) {
                hasSpotifyId = true;
                break;
            }
        }
        // P1902 claim이 없어도 SPARQL로 찾았으므로 일치로 간주
        if (hasSpotifyId || spotifyId != null) {
            score += 100; // 기본 신뢰도
        } else {
            // P1902가 없으면 신뢰도 낮음 (하지만 이름 기반 검색에서는 허용)
            return null;
        }
        
        // 2. P31이 사람/음악가/음악 그룹인지 확인 (+50점)
        List<String> instanceOfList = wikidataClient.getAllEntityIdClaims(entity, "P31");
        boolean isPersonOrMusician = false;
        for (String instanceOf : instanceOfList) {
            if (instanceOf.contains("Q5") || // human
                instanceOf.contains("Q215380") || // musical group
                instanceOf.contains("Q639669") || // musician
                instanceOf.contains("Q177220") || // singer
                instanceOf.contains("Q488205") || // singer-songwriter
                instanceOf.contains("Q10861427")) { // recording artist
                isPersonOrMusician = true;
                break;
            }
        }
        if (isPersonOrMusician) {
            score += 50;
        }
        
        // 3. sitelinks 존재 확인 (특히 ko/en) (+30점)
        com.fasterxml.jackson.databind.JsonNode sitelinks = entity.path("sitelinks");
        if (!sitelinks.isMissingNode() && sitelinks.size() > 0) {
            boolean hasKoOrEnWiki = false;
            if (!sitelinks.path("kowiki").isMissingNode() || 
                !sitelinks.path("enwiki").isMissingNode()) {
                hasKoOrEnWiki = true;
            }
            if (hasKoOrEnWiki) {
                score += 30;
            } else {
                score += 10; // 다른 언어 위키라도 있으면 보너스
            }
        }
        
        // 4. label이 아티스트 이름과 유사한지 확인 (+20점)
        if (artistName != null && !artistName.isBlank()) {
            String koLabel = entity.path("labels").path("ko").path("value").asText(null);
            String enLabel = entity.path("labels").path("en").path("value").asText(null);
            
            String artistNameLower = artistName.toLowerCase().trim();
            if (koLabel != null && koLabel.toLowerCase().contains(artistNameLower)) {
                score += 20;
            } else if (enLabel != null && enLabel.toLowerCase().contains(artistNameLower)) {
                score += 20;
            }
        }
        if (nameKo != null && !nameKo.isBlank()) {
            String koLabel = entity.path("labels").path("ko").path("value").asText(null);
            if (koLabel != null && koLabel.contains(nameKo)) {
                score += 20;
            }
        }
        
        // 5. Description 언어별 보너스 점수
        // 한국어 description (+20점 보너스)
        com.fasterxml.jackson.databind.JsonNode koDesc = descriptions.path("ko");
        if (!koDesc.isMissingNode()) {
            com.fasterxml.jackson.databind.JsonNode value = koDesc.path("value");
            if (!value.isMissingNode() && !value.asText().isBlank()) {
                selectedDescription = value.asText();
                selectedLanguage = "ko";
                score += 20; // 언어 보너스
            }
        }
        
        // 영어 description (+10점 보너스)
        if (selectedDescription == null) {
            com.fasterxml.jackson.databind.JsonNode enDesc = descriptions.path("en");
            if (!enDesc.isMissingNode()) {
                com.fasterxml.jackson.databind.JsonNode value = enDesc.path("value");
                if (!value.isMissingNode() && !value.asText().isBlank()) {
                    selectedDescription = value.asText();
                    selectedLanguage = "en";
                    score += 10; // 언어 보너스
                }
            }
        }
        
        // 기타 언어 description (+5점 보너스)
        if (selectedDescription == null) {
            java.util.Iterator<String> languageIterator = descriptions.fieldNames();
            while (languageIterator.hasNext()) {
                String lang = languageIterator.next();
                if (!lang.equals("ko") && !lang.equals("en")) {
                    com.fasterxml.jackson.databind.JsonNode langDesc = descriptions.path(lang);
                    if (!langDesc.isMissingNode()) {
                        com.fasterxml.jackson.databind.JsonNode value = langDesc.path("value");
                        if (!value.isMissingNode() && !value.asText().isBlank()) {
                            selectedDescription = value.asText();
                            selectedLanguage = lang;
                            score += 5; // 언어 보너스
                            break;
                        }
                    }
                }
            }
        }
        
        // description이 없으면 null 반환
        if (selectedDescription == null) {
            return null;
        }
        
        return new DescriptionCandidate(qid, selectedDescription, selectedLanguage, score);
    }
    
    /**
     * MusicBrainz + Wikidata 메타로 설명 생성
     * 
     * 템플릿:
     * - 솔로: "{국적}의 {직업}로, {활동 시작 연도}년부터 활동 중이다."
     * - 그룹 멤버: "{국적}의 {직업}로, {그룹명}의 멤버다."
     * - 그룹: "{국적}의 {장르/유형} 그룹으로, {활동 시작 연도}년에 결성되었다."
     */
    private Optional<String> generateDescriptionFromMetadata(Artist artist) {
        try {
            // 1단계: Wikidata에서 의미 정보 수집
            Optional<com.back.web7_9_codecrete_be.global.wikidata.WikidataSearchClient.WikidataDescriptionInfo> wdInfoOpt = Optional.empty();
            if (artist.getSpotifyArtistId() != null && !artist.getSpotifyArtistId().isBlank()) {
                Optional<String> qidOpt = wikidataClient.searchWikidataIdBySpotifyId(artist.getSpotifyArtistId());
                if (qidOpt.isPresent()) {
                    wdInfoOpt = wikidataClient.getDescriptionInfoByQid(qidOpt.get());
                }
            }
            
            // 2단계: MusicBrainz에서 정형 정보 수집
            Optional<com.back.web7_9_codecrete_be.global.musicbrainz.MusicBrainzEntityClient.MusicBrainzDetailInfo> mbInfoOpt = Optional.empty();
            if (artist.getMusicBrainzId() != null && !artist.getMusicBrainzId().isBlank()) {
                mbInfoOpt = musicBrainzClient.getArtistDetailInfo(artist.getMusicBrainzId());
            }
            
            // 3단계: 템플릿 기반 설명 생성
            String description = buildDescriptionFromMetadata(wdInfoOpt, mbInfoOpt, artist);
            
            if (description != null && !description.isBlank()) {
                return Optional.of(description);
            }
            
            return Optional.empty();
        } catch (Exception e) {
            log.debug("MusicBrainz+Wikidata 메타로 설명 생성 실패: artistId={}", artist.getId(), e);
            return Optional.empty();
        }
    }
    
    /**
     * 메타데이터로부터 설명 템플릿 생성
     * 
     * ⚠️ 중요: 활동 시작 연도는 무조건 MusicBrainz.life-span.begin만 사용
     * Wikidata 출생일(P569)은 절대 사용하지 않음
     */
    private String buildDescriptionFromMetadata(
            Optional<com.back.web7_9_codecrete_be.global.wikidata.WikidataSearchClient.WikidataDescriptionInfo> wdInfoOpt,
            Optional<com.back.web7_9_codecrete_be.global.musicbrainz.MusicBrainzEntityClient.MusicBrainzDetailInfo> mbInfoOpt,
            Artist artist) {
        
        String nationality = null;
        String occupation = null;
        String group = null;
        String type = null;
        String beginDate = null; // ⚠️ MusicBrainz.life-span.begin만 사용 (Wikidata 출생일 절대 사용 금지)
        
        // Wikidata 정보 추출 (국적, 직업, 그룹만)
        if (wdInfoOpt.isPresent()) {
            com.back.web7_9_codecrete_be.global.wikidata.WikidataSearchClient.WikidataDescriptionInfo wdInfo = wdInfoOpt.get();
            nationality = wdInfo.getNationality();
            occupation = wdInfo.getOccupation();
            group = wdInfo.getGroup();
            // ⚠️ Wikidata 출생일(P569)은 절대 사용하지 않음
        }
        
        // MusicBrainz 정보 추출 (타입, 활동 시작 연도)
        if (mbInfoOpt.isPresent()) {
            com.back.web7_9_codecrete_be.global.musicbrainz.MusicBrainzEntityClient.MusicBrainzDetailInfo mbInfo = mbInfoOpt.get();
            type = mbInfo.getType();
            // ⚠️ 활동 시작 연도는 무조건 MusicBrainz.life-span.begin만 사용
            String rawBeginDate = mbInfo.getBeginDate();
            
            // SOLO 아티스트의 경우, beginDate가 출생일일 가능성 체크
            if (rawBeginDate != null && !rawBeginDate.isBlank() && "SOLO".equals(type)) {
                beginDate = validateBeginDate(rawBeginDate, artist, wdInfoOpt);
            } else {
                beginDate = rawBeginDate;
            }
        }
        
        // 아티스트 타입이 없으면 DB에서 가져오기 (우선순위: MusicBrainz > DB)
        if (type == null && artist.getArtistType() != null) {
            type = artist.getArtistType().name();
        }
        
        // 그룹명이 없으면 DB에서 가져오기
        if (group == null && artist.getArtistGroup() != null && !artist.getArtistGroup().isBlank()) {
            group = artist.getArtistGroup();
        }
        
        // 타입이 여전히 null이면 기본값으로 "SOLO" 사용 (그룹이 아닌 것으로 간주)
        if (type == null) {
            type = "SOLO";
        }
        
        // beginDate 유효성 검사 (null, "null", 빈 문자열, 공백만 있는 경우 제외)
        boolean hasValidBeginDate = beginDate != null 
                && !beginDate.isBlank() 
                && !beginDate.trim().equalsIgnoreCase("null")
                && beginDate.matches("\\d{4}"); // 4자리 숫자만 허용
        
        // 템플릿 생성
        if ("GROUP".equals(type)) {
            // 그룹: "{국적}의 {장르/유형} 그룹으로, {활동 시작 연도}년에 결성되었다." 또는 "{국적}의 {장르/유형} 그룹이다."
            StringBuilder sb = new StringBuilder();
            
            if (nationality != null && !nationality.isBlank()) {
                sb.append(nationality).append("의 ");
            }
            
            // 장르/유형 (직업이 있으면 사용, 없으면 "음악")
            if (occupation != null && !occupation.isBlank()) {
                sb.append(occupation);
            } else {
                sb.append("음악");
            }
            
            if (hasValidBeginDate) {
                sb.append(" 그룹으로, ").append(beginDate.trim()).append("년에 결성되었다.");
            } else {
                sb.append(" 그룹이다.");
            }
            
            return sb.toString();
            
        } else if ("SOLO".equals(type)) {
            // 그룹 멤버인 경우
            if (group != null && !group.isBlank()) {
                // 그룹 멤버: "{국적}의 {직업}로, {그룹명}의 멤버다."
                StringBuilder sb = new StringBuilder();
                
                if (nationality != null && !nationality.isBlank()) {
                    sb.append(nationality).append("의 ");
                }
                
                if (occupation != null && !occupation.isBlank()) {
                    sb.append(occupation);
                } else {
                    sb.append("가수");
                }
                sb.append("로, ").append(group).append("의 멤버다.");
                
                return sb.toString();
            } else {
                // 솔로: "{국적}의 {직업}로, {활동 시작 연도}년부터 활동 중이다."
                StringBuilder sb = new StringBuilder();
                
                if (nationality != null && !nationality.isBlank()) {
                    sb.append(nationality).append("의 ");
                }
                
                if (occupation != null && !occupation.isBlank()) {
                    sb.append(occupation);
                } else {
                    sb.append("가수");
                }
                sb.append("로");
                
                if (hasValidBeginDate) {
                    sb.append(", ").append(beginDate.trim()).append("년부터 활동 중이다.");
                } else {
                    sb.append("이다.");
                }
                
                return sb.toString();
            }
        } else {
            // 타입이 불명확한 경우 기본 템플릿
            StringBuilder sb = new StringBuilder();
            
            if (nationality != null && !nationality.isBlank()) {
                sb.append(nationality).append("의 ");
            }
            
            if (occupation != null && !occupation.isBlank()) {
                sb.append(occupation);
            } else {
                sb.append("아티스트");
            }
            sb.append("이다.");
            
            return sb.toString();
        }
    }
    
    /**
     * beginDate가 출생일인지 검증 (SOLO 아티스트 전용)
     * 
     * MusicBrainz의 life-span.begin이 솔로 아티스트의 경우 출생일일 수 있으므로,
     * Wikidata 출생일(P569)과 비교하여 필터링
     */
    private String validateBeginDate(String beginDate, Artist artist, 
            Optional<com.back.web7_9_codecrete_be.global.wikidata.WikidataSearchClient.WikidataDescriptionInfo> wdInfoOpt) {
        try {
            int beginYear = Integer.parseInt(beginDate);
            int currentYear = java.time.Year.now().getValue();
            
            // 1. 현재로부터 50년 이상 전이면 출생일일 가능성이 높음
            if (beginYear < currentYear - 50) {
                log.debug("beginDate가 너무 오래됨 (출생일 가능성): artistId={}, beginDate={}", artist.getId(), beginDate);
                return null;
            }
            
            // 2. Wikidata에서 출생일(P569) 가져와서 비교
            if (artist.getSpotifyArtistId() != null && !artist.getSpotifyArtistId().isBlank()) {
                Optional<String> qidOpt = wikidataClient.searchWikidataIdBySpotifyId(artist.getSpotifyArtistId());
                if (qidOpt.isPresent()) {
                    Optional<com.fasterxml.jackson.databind.JsonNode> entityOpt = wikidataClient.getEntityInfo(qidOpt.get());
                    if (entityOpt.isPresent()) {
                        com.fasterxml.jackson.databind.JsonNode entity = entityOpt.get();
                        Optional<String> birthDateOpt = wikidataClient.getTimeClaim(entity, "P569"); // P569 = date of birth
                        
                        if (birthDateOpt.isPresent()) {
                            String birthDate = birthDateOpt.get();
                            // "1993-01-12" 형식에서 연도만 추출
                            if (birthDate.length() >= 4) {
                                try {
                                    int birthYear = Integer.parseInt(birthDate.substring(0, 4));
                                    // beginDate가 출생일과 같거나 1-2년 차이면 출생일로 간주
                                    int yearDiff = Math.abs(beginYear - birthYear);
                                    if (yearDiff <= 2) {
                                        log.debug("beginDate가 출생일과 일치 (제외): artistId={}, beginDate={}, birthDate={}", 
                                                artist.getId(), beginDate, birthDate);
                                        return null;
                                    }
                                } catch (NumberFormatException e) {
                                    // 무시
                                }
                            }
                        }
                    }
                }
            }
            
            // 검증 통과
            return beginDate;
        } catch (NumberFormatException e) {
            log.debug("beginDate 파싱 실패: artistId={}, beginDate={}", artist.getId(), beginDate);
            return null;
        } catch (Exception e) {
            log.debug("beginDate 검증 중 오류: artistId={}, beginDate={}", artist.getId(), beginDate, e);
            // 오류 발생 시 원본 반환 (안전하게)
            return beginDate;
        }
    }
    
    /**
     * Wikidata에서 한국어 실명 조회 (P1477 @ko 우선, 없으면 P735+P734 @ko)
     */
    private Optional<String> getWikidataRealNameKo(String spotifyId) {
        try {
            Optional<String> qidOpt = wikidataClient.searchWikidataIdBySpotifyId(spotifyId);
            if (qidOpt.isEmpty()) {
                return Optional.empty();
            }
            
            String qid = qidOpt.get();
            Optional<com.fasterxml.jackson.databind.JsonNode> entityOpt = wikidataClient.getEntityInfo(qid);
            if (entityOpt.isEmpty()) {
                return Optional.empty();
            }
            
            com.fasterxml.jackson.databind.JsonNode entity = entityOpt.get();
            
            // 1. P1477 @ko 우선 확인
            Optional<String> p1477Ko = getP1477ByLanguage(entity, "ko");
            if (p1477Ko.isPresent()) {
                return p1477Ko;
            }
            
            // 2. P735+P734 @ko 조합
            Optional<String> p735P734Ko = getP735P734ByLanguage(entity, "ko");
            if (p735P734Ko.isPresent()) {
                return p735P734Ko;
            }
            
            return Optional.empty();
        } catch (Exception e) {
            log.debug("Wikidata(ko) 실명 조회 실패: spotifyId={}", spotifyId, e);
            return Optional.empty();
        }
    }
    
    /**
     * Wikidata에서 영어 실명 조회 (P1477 @en 우선, 없으면 P735+P734 @en)
     */
    private Optional<String> getWikidataRealNameEn(String spotifyId) {
        try {
            Optional<String> qidOpt = wikidataClient.searchWikidataIdBySpotifyId(spotifyId);
            if (qidOpt.isEmpty()) {
                return Optional.empty();
            }
            
            String qid = qidOpt.get();
            Optional<com.fasterxml.jackson.databind.JsonNode> entityOpt = wikidataClient.getEntityInfo(qid);
            if (entityOpt.isEmpty()) {
                return Optional.empty();
            }
            
            com.fasterxml.jackson.databind.JsonNode entity = entityOpt.get();
            
            // 1. P1477 @en 우선 확인
            Optional<String> p1477En = getP1477ByLanguage(entity, "en");
            if (p1477En.isPresent()) {
                return p1477En;
            }
            
            // 2. P735+P734 @en 조합
            Optional<String> p735P734En = getP735P734ByLanguage(entity, "en");
            if (p735P734En.isPresent()) {
                return p735P734En;
            }
            
            return Optional.empty();
        } catch (Exception e) {
            log.debug("Wikidata(en) 실명 조회 실패: spotifyId={}", spotifyId, e);
            return Optional.empty();
        }
    }
    
    /**
     * P1477 (birth name)을 특정 언어로 조회
     */
    private Optional<String> getP1477ByLanguage(com.fasterxml.jackson.databind.JsonNode entity, String language) {
        try {
            com.fasterxml.jackson.databind.JsonNode claims = entity.path("claims").path("P1477");
            if (!claims.isArray() || claims.isEmpty()) {
                return Optional.empty();
            }
            
            for (com.fasterxml.jackson.databind.JsonNode claim : claims) {
                com.fasterxml.jackson.databind.JsonNode mainsnak = claim.path("mainsnak");
                com.fasterxml.jackson.databind.JsonNode datavalue = mainsnak.path("datavalue");
                com.fasterxml.jackson.databind.JsonNode value = datavalue.path("value");
                
                if (value.isMissingNode()) {
                    continue;
                }
                
                String name = null;
                String lang = null;
                
                // monolingual text 타입
                com.fasterxml.jackson.databind.JsonNode languageNode = value.path("language");
                if (!languageNode.isMissingNode()) {
                    lang = languageNode.asText();
                    com.fasterxml.jackson.databind.JsonNode textNode = value.path("text");
                    if (!textNode.isMissingNode()) {
                        name = textNode.asText();
                    }
                } else {
                    // 일반 string 타입
                    name = value.asText();
                }
                
                if (name != null && !name.isBlank() && language.equals(lang)) {
                    return Optional.of(name);
                }
            }
            
            return Optional.empty();
        } catch (Exception e) {
            log.debug("P1477 언어별 조회 실패: language={}", language, e);
            return Optional.empty();
        }
    }
    
    /**
     * P735+P734 조합을 특정 언어로 조회
     */
    private Optional<String> getP735P734ByLanguage(com.fasterxml.jackson.databind.JsonNode entity, String language) {
        try {
            List<NamePart> givenNames = collectNameParts(entity, "P735", "");
            List<NamePart> familyNames = collectNameParts(entity, "P734", "");
            
            for (NamePart given : givenNames) {
                for (NamePart family : familyNames) {
                    if (given.lang.equals(language) && family.lang.equals(language)) {
                        String combined = family.value + " " + given.value; // 한국/일본식: 성 + 이름
                        return Optional.of(combined);
                    }
                }
            }
            
            return Optional.empty();
        } catch (Exception e) {
            log.debug("P735+P734 언어별 조회 실패: language={}", language, e);
            return Optional.empty();
        }
    }
    
    /**
     * MusicBrainz에서 실명 조회 (기존 로직 사용)
     */
    private Optional<String> getMusicBrainzRealName(String mbid, String stageName) {
        return musicBrainzClient.getRealNameByMbid(mbid, stageName);
    }
    
    /**
     * 실명 후보 수집
     * Wikidata와 MusicBrainz에서 모든 후보를 수집
     */
    private List<RealNameCandidate> collectRealNameCandidates(Artist artist) {
        List<RealNameCandidate> candidates = new ArrayList<>();
        
        // Wikidata 후보 수집
        try {
            candidates.addAll(collectWikidataCandidates(artist.getSpotifyArtistId()));
        } catch (Exception e) {
            log.warn("Wikidata 후보 수집 실패: artistId={}, spotifyId={}", 
                    artist.getId(), artist.getSpotifyArtistId(), e);
        }
        
        // MusicBrainz 후보 수집
        try {
            if (artist.getMusicBrainzId() != null && !artist.getMusicBrainzId().isBlank()) {
                candidates.addAll(collectMusicBrainzCandidates(artist.getMusicBrainzId(), artist.getArtistName()));
            }
        } catch (Exception e) {
            log.warn("MusicBrainz 후보 수집 실패: artistId={}, mbid={}", 
                    artist.getId(), artist.getMusicBrainzId(), e);
        }
        
        return candidates;
    }
    
    /**
     * Wikidata에서 실명 후보 수집
     * P1477 (birth name) @ko/@ja/@en + P735/P734 (이름/성) 조합
     */
    private List<RealNameCandidate> collectWikidataCandidates(String spotifyId) {
        List<RealNameCandidate> candidates = new ArrayList<>();
        
        try {
            // Spotify ID로 QID 찾기
            Optional<String> qidOpt = wikidataClient.searchWikidataIdBySpotifyId(spotifyId);
            if (qidOpt.isEmpty()) {
                return candidates;
            }
            
            String qid = qidOpt.get();
            Optional<com.fasterxml.jackson.databind.JsonNode> entityOpt = wikidataClient.getEntityInfo(qid);
            if (entityOpt.isEmpty()) {
                return candidates;
            }
            
            com.fasterxml.jackson.databind.JsonNode entity = entityOpt.get();
            
            // P1477 (birth name) - 모든 언어 수집
            candidates.addAll(collectP1477Candidates(entity, spotifyId));
            
            // P735 (given name) + P734 (family name) 조합
            candidates.addAll(collectP735P734Candidates(entity, spotifyId));
            
        } catch (Exception e) {
            log.warn("Wikidata 후보 수집 중 예외: spotifyId={}", spotifyId, e);
        }
        
        return candidates;
    }
    
    /**
     * P1477 (birth name) 후보 수집 - 모든 언어
     */
    private List<RealNameCandidate> collectP1477Candidates(
            com.fasterxml.jackson.databind.JsonNode entity, String spotifyId) {
        List<RealNameCandidate> candidates = new ArrayList<>();
        
        try {
            com.fasterxml.jackson.databind.JsonNode claims = entity.path("claims").path("P1477");
            if (!claims.isArray() || claims.isEmpty()) {
                return candidates;
            }
            
            for (com.fasterxml.jackson.databind.JsonNode claim : claims) {
                com.fasterxml.jackson.databind.JsonNode mainsnak = claim.path("mainsnak");
                com.fasterxml.jackson.databind.JsonNode datavalue = mainsnak.path("datavalue");
                com.fasterxml.jackson.databind.JsonNode value = datavalue.path("value");
                
                if (value.isMissingNode()) {
                    continue;
                }
                
                String name = null;
                String lang = null;
                
                // monolingual text 타입
                com.fasterxml.jackson.databind.JsonNode languageNode = value.path("language");
                if (!languageNode.isMissingNode()) {
                    lang = languageNode.asText();
                    com.fasterxml.jackson.databind.JsonNode textNode = value.path("text");
                    if (!textNode.isMissingNode()) {
                        name = textNode.asText();
                    }
                } else {
                    // 일반 string 타입
                    name = value.asText();
                }
                
                if (name != null && !name.isBlank()) {
                    candidates.add(new RealNameCandidate(
                            name,
                            lang != null ? lang : "unknown",
                            "WIKIDATA",
                            "P1477",
                            0, // 점수는 나중에 계산
                            Map.of("qid", entity.path("id").asText())
                    ));
                }
            }
        } catch (Exception e) {
            log.debug("P1477 후보 수집 실패: spotifyId={}", spotifyId, e);
        }
        
        return candidates;
    }
    
    /**
     * P735 (given name) + P734 (family name) 조합 후보 수집
     */
    private List<RealNameCandidate> collectP735P734Candidates(
            com.fasterxml.jackson.databind.JsonNode entity, String spotifyId) {
        List<RealNameCandidate> candidates = new ArrayList<>();
        
        try {
            // P735 (given name) 조회
            List<NamePart> givenNames = collectNameParts(entity, "P735", spotifyId);
            // P734 (family name) 조회
            List<NamePart> familyNames = collectNameParts(entity, "P734", spotifyId);
            
            // 조합 생성 (given + family)
            for (NamePart given : givenNames) {
                for (NamePart family : familyNames) {
                    // 같은 언어끼리만 조합
                    if (given.lang.equals(family.lang)) {
                        String combined = family.value + " " + given.value; // 한국/일본식: 성 + 이름
                        candidates.add(new RealNameCandidate(
                                combined,
                                given.lang,
                                "WIKIDATA",
                                "P735+P734",
                                0,
                                Map.of("given", given.value, "family", family.value)
                        ));
                    }
                }
            }
        } catch (Exception e) {
            log.debug("P735+P734 후보 수집 실패: spotifyId={}", spotifyId, e);
        }
        
        return candidates;
    }
    
    /**
     * P735 또는 P734의 이름 부분 수집
     */
    private List<NamePart> collectNameParts(
            com.fasterxml.jackson.databind.JsonNode entity, String propertyId, String spotifyId) {
        List<NamePart> parts = new ArrayList<>();
        
        try {
            com.fasterxml.jackson.databind.JsonNode claims = entity.path("claims").path(propertyId);
            if (!claims.isArray() || claims.isEmpty()) {
                return parts;
            }
            
            for (com.fasterxml.jackson.databind.JsonNode claim : claims) {
                com.fasterxml.jackson.databind.JsonNode mainsnak = claim.path("mainsnak");
                com.fasterxml.jackson.databind.JsonNode datavalue = mainsnak.path("datavalue");
                com.fasterxml.jackson.databind.JsonNode value = datavalue.path("value");
                
                com.fasterxml.jackson.databind.JsonNode idNode = value.path("id");
                if (idNode.isMissingNode()) {
                    continue;
                }
                
                String qid = idNode.asText();
                
                // QID로 entity 조회하여 label 가져오기
                Optional<com.fasterxml.jackson.databind.JsonNode> nameEntityOpt = wikidataClient.getEntityInfo(qid);
                if (nameEntityOpt.isPresent()) {
                    com.fasterxml.jackson.databind.JsonNode nameEntity = nameEntityOpt.get();
                    com.fasterxml.jackson.databind.JsonNode labels = nameEntity.path("labels");
                    
                    // 한국어 우선
                    com.fasterxml.jackson.databind.JsonNode koLabel = labels.path("ko");
                    if (!koLabel.isMissingNode()) {
                        String label = koLabel.path("value").asText();
                        if (label != null && !label.isBlank()) {
                            parts.add(new NamePart(label, "ko"));
                        }
                    }
                    
                    // 일본어
                    com.fasterxml.jackson.databind.JsonNode jaLabel = labels.path("ja");
                    if (!jaLabel.isMissingNode()) {
                        String label = jaLabel.path("value").asText();
                        if (label != null && !label.isBlank()) {
                            parts.add(new NamePart(label, "ja"));
                        }
                    }
                    
                    // 영어
                    com.fasterxml.jackson.databind.JsonNode enLabel = labels.path("en");
                    if (!enLabel.isMissingNode()) {
                        String label = enLabel.path("value").asText();
                        if (label != null && !label.isBlank()) {
                            parts.add(new NamePart(label, "en"));
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Name part 수집 실패: propertyId={}, spotifyId={}", propertyId, spotifyId, e);
        }
        
        return parts;
    }
    
    /**
     * MusicBrainz에서 실명 후보 수집
     * aliases에서 type/primary/locale 정보 포함
     */
    private List<RealNameCandidate> collectMusicBrainzCandidates(String mbid, String stageName) {
        List<RealNameCandidate> candidates = new ArrayList<>();
        
        try {
            List<MusicBrainzClient.AliasCandidate> aliasCandidates = musicBrainzClient.collectAliasCandidates(mbid);
            
            for (MusicBrainzClient.AliasCandidate alias : aliasCandidates) {
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("type", alias.getType());
                metadata.put("primary", alias.isPrimary());
                metadata.put("locale", alias.getLocale());
                
                candidates.add(new RealNameCandidate(
                        alias.getName(),
                        alias.getLocale() != null && !alias.getLocale().isBlank() ? alias.getLocale() : "unknown",
                        "MUSICBRAINZ",
                        "alias",
                        0,
                        metadata
                ));
            }
        } catch (Exception e) {
            log.warn("MusicBrainz 후보 수집 실패: mbid={}", mbid, e);
        }
        
        return candidates;
    }
    
    /**
     * 후보 정규화
     * - 앞뒤 공백 제거
     * - 괄호/부가설명 제거: "홍길동 (본명)" → "홍길동"
     * - 중복 제거
     * - 너무 짧은 값/기호만 있는 값 제거
     */
    private List<RealNameCandidate> normalizeCandidates(List<RealNameCandidate> candidates) {
        Map<String, RealNameCandidate> normalizedMap = new LinkedHashMap<>();
        
        for (RealNameCandidate candidate : candidates) {
            String normalized = normalizeName(candidate.getValue());
            
            // 너무 짧거나 기호만 있는 값 제거
            if (normalized.length() < 2 || normalized.matches("^[^가-힣a-zA-Z]+$")) {
                continue;
            }
            
            // 중복 제거 (대소문자/공백 차이 무시)
            String key = normalized.toLowerCase().replaceAll("\\s+", "");
            if (!normalizedMap.containsKey(key)) {
                RealNameCandidate normalizedCandidate = new RealNameCandidate(
                        normalized,
                        candidate.getLang(),
                        candidate.getSource(),
                        candidate.getField(),
                        candidate.getScore(),
                        candidate.getMetadata()
                );
                normalizedMap.put(key, normalizedCandidate);
            }
        }
        
        return new ArrayList<>(normalizedMap.values());
    }
    
    /**
     * 이름 정규화
     */
    private String normalizeName(String name) {
        if (name == null || name.isBlank()) {
            return "";
        }
        
        // 앞뒤 공백 제거
        String normalized = name.trim();
        
        // 괄호 및 부가설명 제거: "홍길동 (본명)" → "홍길동"
        normalized = normalized.replaceAll("\\s*\\([^)]*\\)", "");
        normalized = normalized.replaceAll("\\s*\\[[^]]*\\]", "");
        
        // 연속된 공백을 하나로
        normalized = normalized.replaceAll("\\s+", " ");
        
        return normalized.trim();
    }
    
    /**
     * 후보 채점
     */
    private List<RealNameCandidate> scoreCandidates(List<RealNameCandidate> candidates, String stageName) {
        String normalizedStageName = normalizeName(stageName).toLowerCase().replaceAll("\\s+", "");
        
        for (RealNameCandidate candidate : candidates) {
            int score = 0;
            
            // Wikidata P1477 점수
            if ("WIKIDATA".equals(candidate.getSource()) && "P1477".equals(candidate.getField())) {
                String lang = candidate.getLang();
                if ("ko".equals(lang)) {
                    score = 100;
                } else if ("ja".equals(lang)) {
                    score = 90;
                } else if ("en".equals(lang)) {
                    score = 80;
                } else {
                    score = 70; // 기타 언어
                }
            }
            
            // Wikidata P735+P734 점수 (P1477보다 -10점)
            if ("WIKIDATA".equals(candidate.getSource()) && "P735+P734".equals(candidate.getField())) {
                String lang = candidate.getLang();
                if ("ko".equals(lang)) {
                    score = 90;
                } else if ("ja".equals(lang)) {
                    score = 80;
                } else if ("en".equals(lang)) {
                    score = 70;
                } else {
                    score = 60;
                }
            }
            
            // MusicBrainz 점수
            if ("MUSICBRAINZ".equals(candidate.getSource())) {
                score = 75; // 기본 점수
                
                // type이 Legal name / Birth name이면 가산점
                Object typeObj = candidate.getMetadata().get("type");
                if (typeObj != null) {
                    String type = typeObj.toString();
                    if ("Legal name".equals(type) || "Birth name".equals(type)) {
                        score += 10; // 85점
                    }
                }
                
                // primary alias면 가산점
                Object primaryObj = candidate.getMetadata().get("primary");
                if (primaryObj != null && Boolean.TRUE.equals(primaryObj)) {
                    score += 10;
                }
                
                // locale이 ko/ja/en이면 가산점
                String locale = candidate.getLang();
                if ("ko".equals(locale) || "ja".equals(locale) || "en".equals(locale)) {
                    score += 5;
                }
            }
            
            // 활동명과 동일하면 제외 (점수를 0으로)
            String normalizedCandidate = normalizeName(candidate.getValue()).toLowerCase().replaceAll("\\s+", "");
            if (normalizedCandidate.equals(normalizedStageName)) {
                score = 0;
            }
            
            candidate.setScore(score);
        }
        
        return candidates;
    }
    
    /**
     * 실명 후보 클래스
     */
    private static class RealNameCandidate {
        private String value;
        private String lang;
        private String source; // WIKIDATA, MUSICBRAINZ
        private String field; // P1477, P735+P734, alias
        private int score;
        private Map<String, Object> metadata;
        
        public RealNameCandidate(String value, String lang, String source, String field, 
                                int score, Map<String, Object> metadata) {
            this.value = value;
            this.lang = lang;
            this.source = source;
            this.field = field;
            this.score = score;
            this.metadata = metadata != null ? metadata : new HashMap<>();
        }
        
        public String getValue() { return value; }
        public String getLang() { return lang; }
        public String getSource() { return source; }
        public String getField() { return field; }
        public int getScore() { return score; }
        public Map<String, Object> getMetadata() { return metadata; }
        
        public void setScore(int score) { this.score = score; }
    }
    
    /**
     * 이름 부분 (P735/P734용)
     */
    private static class NamePart {
        final String value;
        final String lang;
        
        NamePart(String value, String lang) {
            this.value = value;
            this.lang = lang;
        }
    }
    
    /**
     * 실명 수집 전용 메서드 (별도 호출 가능)
     */
    public int fetchRealNames(int limit) {
        int actualLimit = limit > 0 ? Math.min(limit, 300) : 100;
        // 실명이 없는 아티스트 찾기
        List<Artist> targets = artistRepository.findAll().stream()
                .filter(a -> a.getSpotifyArtistId() != null && !a.getSpotifyArtistId().isBlank())
                .filter(a -> a.getRealName() == null || a.getRealName().isBlank())
                .limit(actualLimit)
                .toList();
        if (targets.isEmpty()) {
            return 0;
        }
        
        int updated = 0;
        for (Artist artist : targets) {
            try {
                fetchRealName(artist);
                artistRepository.save(artist);
                updated++;
                Thread.sleep(1100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.warn("실명 수집 실패: artistId={}", artist.getId(), e);
            }
        }
        
        log.info("실명 수집 완료: 성공={}, 총={}", updated, targets.size());
        return updated;
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

        // 1단계: FLO Client로 한국어 이름 및 그룹 정보 가져오기
        EnrichStepExecutor.EnrichStepResult stepOne = stepExecutor.executeStepOne(artist);
        if (stepOne.nameKo != null) {
            nameKo = stepOne.nameKo;
            source += stepOne.source != null ? stepOne.source : "";
        }
        // FLO에서 그룹 정보도 가져오기 (아직 그룹 정보가 없을 때만)
        if (stepOne.artistGroup != null && artistGroup == null) {
            artistGroup = stepOne.artistGroup;
            source += stepOne.source != null ? stepOne.source : "";
        }

        // 2단계: nameKo 기반 MusicBrainz 검색
        EnrichStepExecutor.EnrichStepResult stepTwo = stepExecutor.executeStepTwo(nameKo, artistType);
        if (stepTwo.artistType != null) {
            artistType = stepTwo.artistType;
            source += stepTwo.source != null ? stepTwo.source : "";
        }

        // 2.5단계: SOLO 확정 이후 그룹 수집 재시도 (초반에 SOLO가 아니어서 스킵된 케이스 복구)
        if ("SOLO".equals(artistType) && artistGroup == null) {
            EnrichStepExecutor.EnrichStepResult retryResult = stepExecutor.retryGroupCollection(artist, artistType);
            if (retryResult.artistGroup != null) {
                artistGroup = retryResult.artistGroup;
                source += retryResult.source != null ? retryResult.source : "";
            }
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
