package com.back.web7_9_codecrete_be.global.wikidata;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * WikidataClient - Facade 패턴으로 분리된 클래스들을 통합
 * 
 * 이 클래스는 하위 호환성을 위해 유지되며, 내부적으로 다음 클래스들을 사용합니다:
 * - WikidataSearchClient: 검색 관련 기능
 * - WikidataEntityClient: Entity 조회 및 Claim 추출
 * - WikipediaClient: Wikipedia 관련 기능
 * - WikidataRealNameClient: 실명 조회 전용
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WikidataClient {

    private final WikidataSearchClient searchClient;
    private final WikidataEntityClient entityClient;
    private final WikipediaClient wikipediaClient;
    private final WikidataRealNameClient realNameClient;

    // ========== 검색 관련 메서드 ==========

    public Optional<String> searchWikidataIdBySpotifyId(String spotifyId) {
        return searchClient.searchWikidataIdBySpotifyId(spotifyId);
    }

    public List<String> searchWikidataIdCandidatesBySpotifyId(String spotifyId) {
        return searchClient.searchWikidataIdCandidatesBySpotifyId(spotifyId);
    }

    public Optional<String> searchWikidataIdByMusicBrainzId(String musicBrainzId) {
        return searchClient.searchWikidataIdByMusicBrainzId(musicBrainzId);
    }

    public List<String> searchGroupBySpotifyId(String spotifyId) {
        return searchClient.searchGroupBySpotifyId(spotifyId);
    }

    public Optional<String> searchWikidataId(String artistName) {
        return searchClient.searchWikidataId(artistName);
    }

    // ========== Entity 관련 메서드 ==========

    public Optional<JsonNode> getEntityInfo(String qid) {
        return entityClient.getEntityInfo(qid);
    }

    public Optional<String> getEntityIdClaim(JsonNode entity, String propertyId) {
        return entityClient.getEntityIdClaim(entity, propertyId);
    }

    public List<String> getAllEntityIdClaims(JsonNode entity, String propertyId) {
        return entityClient.getAllEntityIdClaims(entity, propertyId);
    }

    public Optional<String> getStringClaim(JsonNode entity, String propertyId) {
        return entityClient.getStringClaim(entity, propertyId);
    }

    public Optional<String> getTimeClaim(JsonNode entity, String propertyId) {
        return entityClient.getTimeClaim(entity, propertyId);
    }

    public Optional<String> getEntityLabelClaim(JsonNode entity, String propertyId) {
        return entityClient.getEntityLabelClaim(entity, propertyId);
    }

    // ========== Wikipedia 관련 메서드 ==========

    public Optional<String> getWikipediaKoreanTitle(JsonNode entity) {
        return wikipediaClient.getWikipediaKoreanTitle(entity);
    }

    public Optional<JsonNode> getWikipediaKoreanSummary(String title) {
        return wikipediaClient.getWikipediaKoreanSummary(title);
    }

    public Optional<String> extractKoreanNameFromSummary(JsonNode summary) {
        return wikipediaClient.extractKoreanNameFromSummary(summary);
    }

    public Optional<String> getKoreanNameFromWikipedia(JsonNode entity) {
        return wikipediaClient.getKoreanNameFromWikipedia(entity);
    }

    public Optional<String> searchKoreanNameFromWikipedia(String artistName) {
        return wikipediaClient.searchKoreanNameFromWikipedia(artistName);
    }

    // ========== 실명 조회 관련 메서드 ==========

    public Optional<String> getRealNameKoBySpotifyId(String spotifyId) {
        return realNameClient.getRealNameKoBySpotifyId(spotifyId);
    }

    public Optional<String> getRealNameEnBySpotifyId(String spotifyId) {
        return realNameClient.getRealNameEnBySpotifyId(spotifyId);
    }
}
