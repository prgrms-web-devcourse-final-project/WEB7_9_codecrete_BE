package com.back.web7_9_codecrete_be.domain.artists.service;

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
import java.util.stream.Collectors;

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
        
        // 실명 수집 (1순위: Wikidata, 2순위: MusicBrainz)
        fetchRealName(artist);
        
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
