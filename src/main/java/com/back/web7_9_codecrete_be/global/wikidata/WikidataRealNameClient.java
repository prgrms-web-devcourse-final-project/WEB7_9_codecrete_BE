package com.back.web7_9_codecrete_be.global.wikidata;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class WikidataRealNameClient {

    private final WikidataSearchClient searchClient;
    private final WikidataEntityClient entityClient;

    /**
     * Spotify ID로 실명(birth name) 조회 - 한국어만
     * P1477 = birth name (실명)의 한국어 버전만 반환
     */
    public Optional<String> getRealNameKoBySpotifyId(String spotifyId) {
        try {
            Optional<String> qidOpt = searchClient.searchWikidataIdBySpotifyId(spotifyId);
            if (qidOpt.isEmpty()) {
                log.debug("Spotify ID로 Wikidata QID를 찾을 수 없음: spotifyId={}", spotifyId);
                return Optional.empty();
            }
            
            String qid = qidOpt.get();
            
            Optional<JsonNode> entityOpt = entityClient.getEntityInfo(qid);
            if (entityOpt.isEmpty()) {
                log.debug("Wikidata Entity 정보를 가져올 수 없음: qid={}", qid);
                return Optional.empty();
            }
            
            JsonNode entity = entityOpt.get();
            
            Optional<String> birthNameKoOpt = getBirthNameByLanguage(entity, "P1477", "ko");
            if (birthNameKoOpt.isPresent()) {
                String realName = birthNameKoOpt.get();
                if (realName != null && !realName.isBlank()) {
                    log.info("Wikidata에서 birth name@ko 찾음: spotifyId={}, qid={}, realName={}", 
                            spotifyId, qid, realName);
                    return Optional.of(realName);
                }
            }
            
            log.debug("Wikidata에서 birth name@ko를 찾을 수 없음: spotifyId={}, qid={}", spotifyId, qid);
            return Optional.empty();
            
        } catch (Exception e) {
            log.warn("Spotify ID로 실명(ko) 조회 실패: spotifyId={}", spotifyId, e);
            return Optional.empty();
        }
    }
    
    /**
     * Spotify ID로 실명(birth name) 조회 - 영어만
     * P1477 = birth name (실명)의 영어 버전만 반환
     */
    public Optional<String> getRealNameEnBySpotifyId(String spotifyId) {
        try {
            Optional<String> qidOpt = searchClient.searchWikidataIdBySpotifyId(spotifyId);
            if (qidOpt.isEmpty()) {
                log.debug("Spotify ID로 Wikidata QID를 찾을 수 없음: spotifyId={}", spotifyId);
                return Optional.empty();
            }
            
            String qid = qidOpt.get();
            
            Optional<JsonNode> entityOpt = entityClient.getEntityInfo(qid);
            if (entityOpt.isEmpty()) {
                log.debug("Wikidata Entity 정보를 가져올 수 없음: qid={}", qid);
                return Optional.empty();
            }
            
            JsonNode entity = entityOpt.get();
            
            Optional<String> birthNameEnOpt = getBirthNameByLanguage(entity, "P1477", "en");
            if (birthNameEnOpt.isPresent()) {
                String realName = birthNameEnOpt.get();
                if (realName != null && !realName.isBlank()) {
                    log.info("Wikidata에서 birth name@en 찾음: spotifyId={}, qid={}, realName={}", 
                            spotifyId, qid, realName);
                    return Optional.of(realName);
                }
            }
            
            log.debug("Wikidata에서 birth name@en을 찾을 수 없음: spotifyId={}, qid={}", spotifyId, qid);
            return Optional.empty();
            
        } catch (Exception e) {
            log.warn("Spotify ID로 실명(en) 조회 실패: spotifyId={}", spotifyId, e);
            return Optional.empty();
        }
    }
    
    /**
     * P1477 (birth name)을 특정 언어로 조회
     */
    private Optional<String> getBirthNameByLanguage(JsonNode entity, String propertyId, String language) {
        try {
            JsonNode claims = entity.path("claims").path(propertyId);
            if (!claims.isArray() || claims.isEmpty()) {
                return Optional.empty();
            }
            
            for (JsonNode claim : claims) {
                JsonNode mainsnak = claim.path("mainsnak");
                JsonNode datavalue = mainsnak.path("datavalue");
                JsonNode value = datavalue.path("value");
                
                if (value.isMissingNode()) {
                    continue;
                }
                
                String name = null;
                String claimLanguage = null;
                
                JsonNode languageNode = value.path("language");
                if (!languageNode.isMissingNode()) {
                    claimLanguage = languageNode.asText();
                    JsonNode textNode = value.path("text");
                    if (!textNode.isMissingNode()) {
                        name = textNode.asText();
                    }
                } else {
                    name = value.asText();
                }
                
                if (name == null || name.isBlank()) {
                    continue;
                }
                
                if (language.equals(claimLanguage)) {
                    log.debug("Wikidata birth name ({}) 찾음: {}", language, name);
                    return Optional.of(name);
                }
            }
            
            return Optional.empty();
        } catch (Exception e) {
            log.debug("Birth name 언어별 조회 실패: propertyId={}, language={}", propertyId, language, e);
            return Optional.empty();
        }
    }
}

