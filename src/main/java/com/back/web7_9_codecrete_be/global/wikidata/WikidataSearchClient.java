package com.back.web7_9_codecrete_be.global.wikidata;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class WikidataSearchClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String WIKIDATA_SEARCH_API = "https://www.wikidata.org/w/api.php";
    private static final String SPARQL_ENDPOINT = "https://query.wikidata.org/sparql";

    /**
     * Spotify Artist ID(P1902)로 QID 찾기
     */
    public Optional<String> searchWikidataIdBySpotifyId(String spotifyId) {
        try {
            String escapedSpotifyId = spotifyId.replace("\\", "\\\\").replace("\"", "\\\"");
            String sparqlQuery = String.format(
                    "SELECT ?item WHERE { ?item wdt:P1902 \"%s\" . ?item wdt:P31 ?inst . FILTER(?inst IN (wd:Q5, wd:Q215380)) } LIMIT 10",
                    escapedSpotifyId
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.set("User-Agent", "CodecreteBE/1.0 (Educational Project; +https://github.com/your-repo)");

            String requestBody = "query=" + URLEncoder.encode(sparqlQuery, StandardCharsets.UTF_8) + "&format=json";
            HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

            log.debug("Wikidata SPARQL 쿼리 실행: {}", sparqlQuery);
            ResponseEntity<String> response = restTemplate.postForEntity(SPARQL_ENDPOINT, request, String.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.warn("Wikidata SPARQL API 응답 실패: status={}", response.getStatusCode());
                return Optional.empty();
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode bindings = root.path("results").path("bindings");

            if (!bindings.isArray() || bindings.isEmpty()) {
                log.debug("Spotify ID로 Wikidata 결과 없음: {}", spotifyId);
                return Optional.empty();
            }

            List<String> candidateQids = new ArrayList<>();
            for (JsonNode binding : bindings) {
                String itemUri = binding.path("item").path("value").asText();
                if (itemUri != null && !itemUri.isBlank()) {
                    String qid = itemUri.substring(itemUri.lastIndexOf("/") + 1);
                    candidateQids.add(qid);
                }
            }

            if (candidateQids.isEmpty()) {
                log.warn("Wikidata URI가 비어있음: spotifyId={}", spotifyId);
                return Optional.empty();
            }

            log.debug("Spotify ID로 Wikidata 후보 {}개 발견: {}", candidateQids.size(), candidateQids);
            String qid = candidateQids.get(0);
            log.info("Spotify ID로 Wikidata ID 찾음: {} -> {} (후보 {}개 중 첫 번째)", spotifyId, qid, candidateQids.size());
            return Optional.of(qid);

        } catch (Exception e) {
            log.error("Spotify ID로 Wikidata 검색 중 예외 발생: spotifyId={}", spotifyId, e);
            return Optional.empty();
        }
    }

    /**
     * Spotify Artist ID로 QID 후보 리스트 찾기 (검증용)
     */
    public List<String> searchWikidataIdCandidatesBySpotifyId(String spotifyId) {
        try {
            String escapedSpotifyId = spotifyId.replace("\\", "\\\\").replace("\"", "\\\"");
            String sparqlQuery = String.format(
                    "SELECT ?item WHERE { ?item wdt:P1902 \"%s\" . ?item wdt:P31 ?inst . FILTER(?inst IN (wd:Q5, wd:Q215380)) } LIMIT 10",
                    escapedSpotifyId
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.set("User-Agent", "CodecreteBE/1.0 (Educational Project; +https://github.com/your-repo)");

            String requestBody = "query=" + URLEncoder.encode(sparqlQuery, StandardCharsets.UTF_8) + "&format=json";
            HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(SPARQL_ENDPOINT, request, String.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return new ArrayList<>();
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode bindings = root.path("results").path("bindings");

            if (!bindings.isArray() || bindings.isEmpty()) {
                return new ArrayList<>();
            }

            List<String> candidateQids = new ArrayList<>();
            for (JsonNode binding : bindings) {
                String itemUri = binding.path("item").path("value").asText();
                if (itemUri != null && !itemUri.isBlank()) {
                    String qid = itemUri.substring(itemUri.lastIndexOf("/") + 1);
                    candidateQids.add(qid);
                }
            }

            return candidateQids;

        } catch (Exception e) {
            log.error("Spotify ID로 Wikidata 후보 검색 중 예외 발생: spotifyId={}", spotifyId, e);
            return new ArrayList<>();
        }
    }

    /**
     * MusicBrainz ID(P434)로 QID 찾기
     */
    public Optional<String> searchWikidataIdByMusicBrainzId(String musicBrainzId) {
        try {
            String escapedMbid = musicBrainzId.replace("\\", "\\\\").replace("\"", "\\\"");
            String sparqlQuery = String.format(
                    "SELECT ?item WHERE { ?item wdt:P434 \"%s\" } LIMIT 1",
                    escapedMbid
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.set("User-Agent", "CodecreteBE/1.0 (Educational Project; +https://github.com/your-repo)");

            String requestBody = "query=" + URLEncoder.encode(sparqlQuery, StandardCharsets.UTF_8) + "&format=json";
            HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

            log.debug("Wikidata SPARQL 쿼리 실행: {}", sparqlQuery);
            ResponseEntity<String> response = restTemplate.postForEntity(SPARQL_ENDPOINT, request, String.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.warn("Wikidata SPARQL API 응답 실패: status={}", response.getStatusCode());
                return Optional.empty();
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode bindings = root.path("results").path("bindings");

            if (!bindings.isArray() || bindings.isEmpty()) {
                log.debug("MusicBrainz ID로 Wikidata 결과 없음: {}", musicBrainzId);
                return Optional.empty();
            }

            String itemUri = bindings.get(0).path("item").path("value").asText();
            if (itemUri == null || itemUri.isBlank()) {
                log.warn("Wikidata URI가 비어있음: musicBrainzId={}", musicBrainzId);
                return Optional.empty();
            }

            String qid = itemUri.substring(itemUri.lastIndexOf("/") + 1);
            log.info("MusicBrainz ID로 Wikidata ID 찾음: {} -> {}", musicBrainzId, qid);
            return Optional.of(qid);

        } catch (Exception e) {
            log.error("MusicBrainz ID로 Wikidata 검색 중 예외 발생: musicBrainzId={}", musicBrainzId, e);
            return Optional.empty();
        }
    }

    /**
     * Spotify ID로 소속 그룹 찾기 (SPARQL)
     */
    public List<String> searchGroupBySpotifyId(String spotifyId) {
        try {
            String escapedSpotifyId = spotifyId.replace("\\", "\\\\").replace("\"", "\\\"");
            String sparqlQuery = String.format(
                    "SELECT ?group ?groupLabel WHERE { " +
                    "?artist wdt:P1902 \"%s\" . " +
                    "?artist wdt:P463 ?group . " +
                    "?group wdt:P31 wd:Q215380 . " +
                    "SERVICE wikibase:label { bd:serviceParam wikibase:language \"ko,en\". } " +
                    "} LIMIT 10",
                    escapedSpotifyId
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.set("User-Agent", "CodecreteBE/1.0 (Educational Project; +https://github.com/your-repo)");

            String requestBody = "query=" + URLEncoder.encode(sparqlQuery, StandardCharsets.UTF_8) + "&format=json";
            HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

            log.debug("Wikidata 소속 그룹 SPARQL 쿼리 실행: {}", sparqlQuery);
            ResponseEntity<String> response = restTemplate.postForEntity(SPARQL_ENDPOINT, request, String.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.warn("Wikidata SPARQL API 응답 실패: status={}", response.getStatusCode());
                return new ArrayList<>();
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode bindings = root.path("results").path("bindings");

            if (!bindings.isArray() || bindings.isEmpty()) {
                log.debug("Spotify ID로 소속 그룹 결과 없음: {}", spotifyId);
                return new ArrayList<>();
            }

            List<String> groupNames = new ArrayList<>();
            for (JsonNode binding : bindings) {
                JsonNode groupLabelNode = binding.path("groupLabel");
                if (!groupLabelNode.isMissingNode()) {
                    JsonNode valueNode = groupLabelNode.path("value");
                    if (!valueNode.isMissingNode()) {
                        String groupName = valueNode.asText();
                        if (groupName != null && !groupName.isBlank()) {
                            groupNames.add(groupName);
                        }
                    }
                }
            }

            log.debug("Spotify ID로 소속 그룹 {}개 발견: spotifyId={}, groups={}", 
                    groupNames.size(), spotifyId, groupNames);
            return groupNames;

        } catch (Exception e) {
            log.error("Spotify ID로 소속 그룹 검색 중 예외 발생: spotifyId={}", spotifyId, e);
            return new ArrayList<>();
        }
    }

    /**
     * 이름으로 QID 찾기 (fallback)
     */
    public Optional<String> searchWikidataId(String artistName) {
        try {
            Map<String, String> params = new HashMap<>();
            params.put("action", "wbsearchentities");
            params.put("search", URLEncoder.encode(artistName, StandardCharsets.UTF_8));
            params.put("language", "ko");
            params.put("format", "json");
            params.put("type", "item");
            params.put("limit", "10");

            String url = WIKIDATA_SEARCH_API + "?" + buildQueryString(params);

            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return Optional.empty();
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode search = root.path("search");
            if (!search.isArray() || search.isEmpty()) return Optional.empty();

            return findBestMatch(search, artistName);
        } catch (Exception e) {
            log.warn("이름으로 Wikidata 검색 실패: {}", artistName, e);
            return Optional.empty();
        }
    }

    /**
     * 여러 검색 결과 중 가장 적합한 Wikidata ID 선택
     */
    private Optional<String> findBestMatch(JsonNode searchResults, String originalName) {
        String bestQid = null;
        int bestScore = -1;
        String bestMatchInfo = null;

        for (JsonNode result : searchResults) {
            String qid = result.path("id").asText();
            if (qid == null || qid.isBlank()) continue;

            String label = result.path("label").asText();
            String description = result.path("description").asText("");

            // Entity 정보는 호출하는 쪽에서 가져와야 함 (순환 참조 방지)
            // 여기서는 간단한 점수 계산만 수행
            int score = calculateMatchScore(null, originalName, label, description);

            if (score > bestScore) {
                bestScore = score;
                bestQid = qid;
                bestMatchInfo = String.format("label=%s, score=%d", label, score);
            }
        }

        if (bestQid != null && bestScore >= 30) {
            log.debug("이름 검색 결과 중 최적 매칭: {} -> {} ({})", originalName, bestQid, bestMatchInfo);
            return Optional.of(bestQid);
        }

        if (bestQid != null) {
            log.debug("이름 검색 결과 점수가 낮아 제외: {} -> {} (score={}, 최소=30)", originalName, bestQid, bestScore);
        }

        return Optional.empty();
    }

    private int calculateMatchScore(JsonNode entity, String originalName, String label, String description) {
        int score = 0;
        String originalLower = originalName.toLowerCase().trim();
        String labelLower = label.toLowerCase().trim();

        if (originalLower.equals(labelLower)) {
            score += 50;
        } else if (originalLower.contains(labelLower) || labelLower.contains(originalLower)) {
            score += 30;
        } else if (hasWordMatch(originalLower, labelLower)) {
            score += 20;
        }

        // Entity 정보가 있으면 추가 점수 계산
        if (entity != null) {
            JsonNode enLabel = entity.path("labels").path("en").path("value");
            if (!enLabel.isMissingNode()) {
                String enValue = enLabel.asText().toLowerCase().trim();
                if (originalLower.equals(enValue)) {
                    score += 40;
                } else if (originalLower.contains(enValue) || enValue.contains(originalLower)) {
                    score += 25;
                }
            }

            JsonNode claims = entity.path("claims").path("P345");
            if (claims.isArray() && claims.size() > 0) {
                score += 30;
            }
        }

        String descLower = description.toLowerCase();
        if (descLower.contains("singer") || descLower.contains("musician") || 
            descLower.contains("artist") || descLower.contains("group") ||
            descLower.contains("가수") || descLower.contains("음악") || descLower.contains("아티스트")) {
            score += 10;
        }

        return Math.min(score, 100);
    }

    private boolean hasWordMatch(String str1, String str2) {
        String[] words1 = str1.split("[\\s\\-_]+");
        String[] words2 = str2.split("[\\s\\-_]+");
        
        for (String w1 : words1) {
            for (String w2 : words2) {
                if (w1.length() >= 3 && w2.length() >= 3 && 
                    (w1.equals(w2) || w1.contains(w2) || w2.contains(w1))) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isCommonKoreanWord(String word) {
        if (word == null || word.length() <= 2) return true;

        String[] commonWords = {"보물", "사랑", "빛", "별", "꿈", "하늘", "바다", "땅", "물", "불", "바람"};
        for (String common : commonWords) {
            if (word.equals(common)) {
                return true;
            }
        }

        if (word.length() <= 3 && word.matches("^[가-힣]+$")) {
            return true;
        }
        
        return false;
    }

    private String buildQueryString(Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (sb.length() > 0) sb.append("&");
            sb.append(entry.getKey()).append("=").append(entry.getValue());
        }
        return sb.toString();
    }
}

