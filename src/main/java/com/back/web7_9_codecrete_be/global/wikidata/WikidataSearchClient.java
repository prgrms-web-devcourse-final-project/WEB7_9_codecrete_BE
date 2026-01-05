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
    private final WikidataEntityClient entityClient; // For getEntityInfo and description

    private static final String WIKIDATA_SEARCH_API = "https://www.wikidata.org/w/api.php";
    private static final String SPARQL_ENDPOINT = "https://query.wikidata.org/sparql";

    /**
     * Spotify Artist ID(P1902)로 QID 찾기
     * 
     * P31 필터를 완화하여 P31(instance of) 또는 P279*(subclass of)로 Q5(인간) 또는 Q215380(음악 그룹)인 엔티티를 찾음
     * 여러 후보를 반환하여 description 스코어링에 사용
     */
    public List<String> searchWikidataIdsBySpotifyId(String spotifyId) {
        try {
            String escapedSpotifyId = spotifyId.replace("\\", "\\\\").replace("\"", "\\\"");
            // QID 후보 검색 완화: P1902만 확인하고, P31 필터는 제거하여 더 넓게 수집
            // 정답 판별은 스코어링 단계에서 수행
            String sparqlQuery = String.format(
                    "SELECT ?item WHERE { " +
                    "?item wdt:P1902 \"%s\" . " +
                    "} LIMIT 30",
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
                return new ArrayList<>();
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode bindings = root.path("results").path("bindings");

            if (!bindings.isArray() || bindings.isEmpty()) {
                log.debug("Spotify ID로 Wikidata 결과 없음: {}", spotifyId);
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

            if (candidateQids.isEmpty()) {
                log.warn("Wikidata URI가 비어있음: spotifyId={}", spotifyId);
                return new ArrayList<>();
            }

            log.debug("Spotify ID로 Wikidata 후보 {}개 발견: {}", candidateQids.size(), candidateQids);
            return candidateQids;

        } catch (Exception e) {
            log.error("Spotify ID로 Wikidata 검색 중 예외 발생: spotifyId={}", spotifyId, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Spotify Artist ID(P1902)로 QID 찾기 (단일 반환, 하위 호환성)
     * 
     * @deprecated 여러 후보를 반환하는 searchWikidataIdsBySpotifyId 사용 권장
     */
    @Deprecated
    public Optional<String> searchWikidataIdBySpotifyId(String spotifyId) {
        List<String> candidates = searchWikidataIdsBySpotifyId(spotifyId);
        if (candidates.isEmpty()) {
            return Optional.empty();
        }
        String qid = candidates.get(0);
        log.info("Spotify ID로 Wikidata ID 찾음: {} -> {} (후보 {}개 중 첫 번째)", spotifyId, qid, candidates.size());
        return Optional.of(qid);
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
    
    /**
     * Wikidata에서 의미 정보 수집 (SPARQL)
     * P27 (국적), P106 (직업), P463 (그룹 소속)
     * description은 Entity API에서 직접 가져오기
     */
    public Optional<WikidataDescriptionInfo> getDescriptionInfoByQid(String qid) {
        try {
            String escapedQid = qid.replace("\\", "\\\\").replace("\"", "\\\"");
            String sparqlQuery = String.format(
                    "SELECT ?nationalityLabel ?occupationLabel ?groupLabel WHERE { " +
                    "OPTIONAL { wd:%s wdt:P27 ?nationality . } " +
                    "OPTIONAL { wd:%s wdt:P106 ?occupation . } " +
                    "OPTIONAL { wd:%s wdt:P463 ?group . } " +
                    "SERVICE wikibase:label { bd:serviceParam wikibase:language \"ko,en\". } " +
                    "} LIMIT 1",
                    escapedQid, escapedQid, escapedQid
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.set("User-Agent", "CodecreteBE/1.0 (Educational Project; +https://github.com/your-repo)");

            String requestBody = "query=" + URLEncoder.encode(sparqlQuery, StandardCharsets.UTF_8) + "&format=json";
            HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(SPARQL_ENDPOINT, request, String.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.debug("Wikidata 설명 정보 SPARQL 실패: qid={}, status={}", qid, response.getStatusCode());
                return Optional.empty();
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode bindings = root.path("results").path("bindings");

            if (!bindings.isArray() || bindings.isEmpty()) {
                return Optional.empty();
            }

            JsonNode binding = bindings.get(0);
            
            // description은 Entity API에서 직접 가져오기 (SPARQL에서는 schema:description 사용 불가)
            String description = null;
            try {
                Optional<com.fasterxml.jackson.databind.JsonNode> entityOpt = entityClient.getEntityInfo(qid);
                if (entityOpt.isPresent()) {
                    com.fasterxml.jackson.databind.JsonNode entity = entityOpt.get();
                    com.fasterxml.jackson.databind.JsonNode descriptions = entity.path("descriptions");
                    
                    // 한국어 description 우선
                    com.fasterxml.jackson.databind.JsonNode koDesc = descriptions.path("ko");
                    if (!koDesc.isMissingNode()) {
                        com.fasterxml.jackson.databind.JsonNode value = koDesc.path("value");
                        if (!value.isMissingNode() && !value.asText().isBlank()) {
                            description = value.asText();
                        }
                    }
                    
                    // 한국어가 없으면 영어 description
                    if (description == null) {
                        com.fasterxml.jackson.databind.JsonNode enDesc = descriptions.path("en");
                        if (!enDesc.isMissingNode()) {
                            com.fasterxml.jackson.databind.JsonNode value = enDesc.path("value");
                            if (!value.isMissingNode() && !value.asText().isBlank()) {
                                description = value.asText();
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.debug("Entity API에서 description 가져오기 실패: qid={}", qid, e);
            }
            
            String nationality = null;
            JsonNode nationalityNode = binding.path("nationalityLabel");
            if (!nationalityNode.isMissingNode()) {
                JsonNode value = nationalityNode.path("value");
                if (!value.isMissingNode() && !value.asText().isBlank()) {
                    nationality = value.asText();
                }
            }
            
            String occupation = null;
            JsonNode occupationNode = binding.path("occupationLabel");
            if (!occupationNode.isMissingNode()) {
                JsonNode value = occupationNode.path("value");
                if (!value.isMissingNode() && !value.asText().isBlank()) {
                    occupation = value.asText();
                }
            }
            
            String group = null;
            JsonNode groupNode = binding.path("groupLabel");
            if (!groupNode.isMissingNode()) {
                JsonNode value = groupNode.path("value");
                if (!value.isMissingNode() && !value.asText().isBlank()) {
                    group = value.asText();
                }
            }
            
            return Optional.of(new WikidataDescriptionInfo(description, nationality, occupation, group));
            
        } catch (Exception e) {
            log.debug("Wikidata 설명 정보 수집 실패: qid={}", qid, e);
            return Optional.empty();
        }
    }
    
    /**
     * Wikidata 설명 정보
     */
    public static class WikidataDescriptionInfo {
        private final String description;
        private final String nationality;
        private final String occupation;
        private final String group;
        
        public WikidataDescriptionInfo(String description, String nationality, String occupation, String group) {
            this.description = description;
            this.nationality = nationality;
            this.occupation = occupation;
            this.group = group;
        }
        
        public String getDescription() { return description; }
        public String getNationality() { return nationality; }
        public String getOccupation() { return occupation; }
        public String getGroup() { return group; }
    }
}

