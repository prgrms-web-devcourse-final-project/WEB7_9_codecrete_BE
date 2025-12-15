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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class WikidataClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String WIKIDATA_SEARCH_API = "https://www.wikidata.org/w/api.php";
    private static final String WIKIDATA_ENTITY_API = "https://www.wikidata.org/wiki/Special:EntityData/";
    private static final String WIKIPEDIA_KO_API = "https://ko.wikipedia.org/api/rest_v1/page/summary/";

    // Spotify Artist ID(P345)로 QID 찾기
    public Optional<String> searchWikidataIdBySpotifyId(String spotifyId) {
        try {
            // P345 = Spotify artist ID
            // P1902 = Spotify album ID (잘못된 속성)
            String escapedSpotifyId = spotifyId.replace("\\", "\\\\").replace("\"", "\\\"");
            String sparqlQuery = String.format(
                    "SELECT ?item WHERE { ?item wdt:P345 \"%s\" } LIMIT 1",
                    escapedSpotifyId
            );

            String url = "https://query.wikidata.org/sparql";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.set("User-Agent", "CodecreteBE/1.0 (Educational Project; +https://github.com/your-repo)");
            
            String requestBody = "query=" + URLEncoder.encode(sparqlQuery, StandardCharsets.UTF_8) + "&format=json";
            
            HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

            log.debug("Wikidata SPARQL 쿼리 실행: {}", sparqlQuery);
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

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

            String itemUri = bindings.get(0).path("item").path("value").asText();
            if (itemUri == null || itemUri.isBlank()) {
                log.warn("Wikidata URI가 비어있음: spotifyId={}", spotifyId);
                return Optional.empty();
            }

            String qid = itemUri.substring(itemUri.lastIndexOf("/") + 1);
            log.info("Spotify ID로 Wikidata ID 찾음: {} -> {}", spotifyId, qid);
            return Optional.of(qid);

        } catch (Exception e) {
            log.error("Spotify ID로 Wikidata 검색 중 예외 발생: spotifyId={}", spotifyId, e);
            return Optional.empty();
        }
    }

    // 이름으로 QID 찾기 (fallback)
    public Optional<String> searchWikidataId(String artistName) {
        try {
            Map<String, String> params = new HashMap<>();
            params.put("action", "wbsearchentities");
            params.put("search", URLEncoder.encode(artistName, StandardCharsets.UTF_8));
            params.put("language", "ko");   // ko 우선
            params.put("format", "json");
            params.put("type", "item");
            params.put("limit", "10");  // 여러 후보 확인

            String url = WIKIDATA_SEARCH_API + "?" + buildQueryString(params);

            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return Optional.empty();
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode search = root.path("search");
            if (!search.isArray() || search.isEmpty()) return Optional.empty();

            // 여러 후보 중 가장 적합한 것 선택
            return findBestMatch(search, artistName);
        } catch (Exception e) {
            log.warn("이름으로 Wikidata 검색 실패: {}", artistName, e);
            return Optional.empty();
        }
    }

    // 여러 검색 결과 중 가장 적합한 Wikidata ID 선택
    private Optional<String> findBestMatch(JsonNode searchResults, String originalName) {
        String bestQid = null;
        int bestScore = -1;
        String bestMatchInfo = null;

        for (JsonNode result : searchResults) {
            String qid = result.path("id").asText();
            if (qid == null || qid.isBlank()) continue;

            String label = result.path("label").asText();
            String description = result.path("description").asText("");

            // 엔티티 정보 가져와서 상세 검증
            Optional<JsonNode> entityOpt = getEntityInfo(qid);
            if (entityOpt.isEmpty()) continue;

            JsonNode entity = entityOpt.get();
            int score = calculateMatchScore(entity, originalName, label, description);

            if (score > bestScore) {
                bestScore = score;
                bestQid = qid;
                bestMatchInfo = String.format("label=%s, score=%d", label, score);
            }
        }

        if (bestQid != null && bestScore >= 30) {  // 최소 점수 기준
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

        // 1. 정확히 일치 (50점)
        if (originalLower.equals(labelLower)) {
            score += 50;
        }
        // 2. 부분 일치 (30점)
        else if (originalLower.contains(labelLower) || labelLower.contains(originalLower)) {
            score += 30;
        }
        // 3. 단어 단위 일치 (20점)
        else if (hasWordMatch(originalLower, labelLower)) {
            score += 20;
        }

        // 4. 영어 레이블 확인
        JsonNode enLabel = entity.path("labels").path("en").path("value");
        if (!enLabel.isMissingNode()) {
            String enValue = enLabel.asText().toLowerCase().trim();
            if (originalLower.equals(enValue)) {
                score += 40;  // 영어 이름 정확 일치
            } else if (originalLower.contains(enValue) || enValue.contains(originalLower)) {
                score += 25;  // 영어 이름 부분 일치
            }
        }

        // 5. Spotify ID가 있으면 보너스 (가장 확실한 방법)
        JsonNode claims = entity.path("claims").path("P345");
        if (claims.isArray() && claims.size() > 0) {
            score += 30;  // Spotify ID가 있으면 높은 신뢰도
        }

        // 6. Wikipedia 한국어 이름이 있으면 보너스
        Optional<String> wikiKoNameOpt = getKoreanNameFromWikipedia(entity);
        if (wikiKoNameOpt.isPresent()) {
            String wikiKoName = wikiKoNameOpt.get();
            // Wikipedia 한국어 이름이 일반 단어가 아니면 보너스
            if (!isCommonKoreanWord(wikiKoName)) {
                score += 15;  // Wikipedia는 더 신뢰도가 높으므로 점수 증가
            } else {
                score -= 20;  // 일반 단어면 감점
            }
        }

        // 7. 설명(description)에 아티스트 관련 키워드가 있으면 보너스
        String descLower = description.toLowerCase();
        if (descLower.contains("singer") || descLower.contains("musician") || 
            descLower.contains("artist") || descLower.contains("group") ||
            descLower.contains("가수") || descLower.contains("음악") || descLower.contains("아티스트")) {
            score += 10;
        }

        return Math.min(score, 100);  // 최대 100점
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

    public Optional<JsonNode> getEntityInfo(String qid) {
        try {
            String url = WIKIDATA_ENTITY_API + qid + ".json";
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return Optional.empty();
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode entity = root.path("entities").path(qid);
            if (entity.isMissingNode() || entity.isNull()) return Optional.empty();

            return Optional.of(entity);
        } catch (Exception e) {
            log.warn("Wikidata entity 조회 실패: {}", qid, e);
            return Optional.empty();
        }
    }


    private boolean isCommonKoreanWord(String word) {
        if (word == null || word.length() <= 2) return true; // 2자 이하는 일반 단어로 간주

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

    public Optional<String> getEntityIdClaim(JsonNode entity, String propertyId) {
        JsonNode claims = entity.path("claims").path(propertyId);
        if (!claims.isArray() || claims.isEmpty()) return Optional.empty();

        JsonNode value = claims.get(0)
                .path("mainsnak")
                .path("datavalue")
                .path("value");

        JsonNode idNode = value.path("id");
        if (!idNode.isMissingNode() && !idNode.asText().isBlank()) {
            return Optional.of(idNode.asText());
        }
        return Optional.empty();
    }

    // claims[propertyId]의 모든 QID 값 반환
    public List<String> getAllEntityIdClaims(JsonNode entity, String propertyId) {
        List<String> results = new java.util.ArrayList<>();
        JsonNode claims = entity.path("claims").path(propertyId);
        if (!claims.isArray() || claims.isEmpty()) return results;

        for (JsonNode claim : claims) {
            JsonNode value = claim
                    .path("mainsnak")
                    .path("datavalue")
                    .path("value");

            JsonNode idNode = value.path("id");
            if (!idNode.isMissingNode() && !idNode.asText().isBlank()) {
                results.add(idNode.asText());
            }
        }
        return results;
    }

    public Optional<String> getWikipediaKoreanTitle(JsonNode entity) {
        try {
            // sitelinks에서 한국어 Wikipedia 페이지 제목 가져오기
            JsonNode sitelinks = entity.path("sitelinks");
            JsonNode koWiki = sitelinks.path("kowiki");
            
            if (!koWiki.isMissingNode()) {
                JsonNode title = koWiki.path("title");
                if (!title.isMissingNode() && !title.asText().isBlank()) {
                    return Optional.of(title.asText());
                }
            }
            
            return Optional.empty();
        } catch (Exception e) {
            log.warn("Wikipedia 한국어 제목 가져오기 실패", e);
            return Optional.empty();
        }
    }

    public Optional<JsonNode> getWikipediaKoreanSummary(String title) {
        try {
            // URLEncoder는 공백을 +로 변환하지만, Wikipedia API는 %20을 선호
            // 공백을 %20으로 변환하기 위해 replace 사용
            String encodedTitle = URLEncoder.encode(title, StandardCharsets.UTF_8)
                    .replace("+", "%20");
            String url = WIKIPEDIA_KO_API + encodedTitle;
            
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                int statusCode = response.getStatusCode().value();
                // 403, 404는 정상적인 실패 케이스 (페이지 없음, 접근 불가 등)
                if (statusCode == 403 || statusCode == 404) {
                    log.debug("Wikipedia 한국어 API 응답 실패: title={}, status={} (페이지 없음 또는 접근 불가)", title, statusCode);
                } else {
                    log.debug("Wikipedia 한국어 API 응답 실패: title={}, status={}", title, statusCode);
                }
                return Optional.empty();
            }
            
            JsonNode summary = objectMapper.readTree(response.getBody());
            return Optional.of(summary);
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            // 403, 404는 정상적인 실패 케이스 (페이지 없음, 접근 불가 등)
            int statusCode = e.getStatusCode().value();
            if (statusCode == 403 || statusCode == 404) {
                log.debug("Wikipedia 한국어 API HTTP 에러: title={}, status={} (페이지 없음 또는 접근 불가)", title, statusCode);
            } else {
                log.warn("Wikipedia 한국어 summary 가져오기 실패: title={}, status={}", title, statusCode, e);
            }
            return Optional.empty();
        } catch (Exception e) {
            log.warn("Wikipedia 한국어 summary 가져오기 실패: title={}", title, e);
            return Optional.empty();
        }
    }

    public Optional<String> extractKoreanNameFromSummary(JsonNode summary) {
        try {
            // 1. title 필드 확인 (페이지 제목)
            JsonNode title = summary.path("title");
            if (!title.isMissingNode() && !title.asText().isBlank()) {
                String titleText = title.asText().trim();
                // 제목에서 괄호 내용 제거 (예: "방탄소년단 (음악 그룹)" -> "방탄소년단")
                String cleanTitle = titleText.replaceAll("\\([^)]*\\)", "").trim();
                if (!cleanTitle.isBlank()) {
                    return Optional.of(cleanTitle);
                }
                return Optional.of(titleText);
            }
            
            // 2. extract 필드에서 첫 문장 확인
            JsonNode extract = summary.path("extract");
            if (!extract.isMissingNode() && !extract.asText().isBlank()) {
                String extractText = extract.asText();
                // 첫 문장에서 이름 추출 시도
                // 예: "방탄소년단은..." -> "방탄소년단"
                String firstSentence = extractText.split("[。\\.]")[0].trim();
                if (firstSentence.length() > 0 && firstSentence.length() <= 20) {
                    // 한글이 포함되어 있고 적절한 길이면 사용
                    if (firstSentence.matches(".*[가-힣].*")) {
                        return Optional.of(firstSentence);
                    }
                }
            }
            
            return Optional.empty();
        } catch (Exception e) {
            log.warn("Wikipedia summary에서 한국어 이름 추출 실패", e);
            return Optional.empty();
        }
    }

    public Optional<String> getKoreanNameFromWikipedia(JsonNode entity) {
        try {
            // 1. Wikipedia 한국어 페이지 제목 가져오기
            Optional<String> wikiTitleOpt = getWikipediaKoreanTitle(entity);
            if (wikiTitleOpt.isEmpty()) {
                log.debug("Wikipedia 한국어 페이지 제목 없음");
                return Optional.empty();
            }
            
            String wikiTitle = wikiTitleOpt.get();
            log.debug("Wikipedia 한국어 페이지 제목: {}", wikiTitle);
            
            // 2. Wikipedia summary 가져오기
            Optional<JsonNode> summaryOpt = getWikipediaKoreanSummary(wikiTitle);
            if (summaryOpt.isEmpty()) {
                log.debug("Wikipedia 한국어 summary 없음: {}", wikiTitle);
                // summary가 없어도 제목 자체를 사용
                return Optional.of(wikiTitle);
            }
            
            JsonNode summary = summaryOpt.get();
            
            // 3. summary에서 한국어 이름 추출
            Optional<String> nameOpt = extractKoreanNameFromSummary(summary);
            if (nameOpt.isPresent()) {
                log.debug("Wikipedia summary에서 한국어 이름 추출: {}", nameOpt.get());
                return nameOpt;
            }
            
            // 4. summary에서 추출 실패하면 제목 사용
            log.debug("Wikipedia summary에서 이름 추출 실패, 제목 사용: {}", wikiTitle);
            return Optional.of(wikiTitle);
            
        } catch (Exception e) {
            log.warn("Wikipedia에서 한국어 이름 가져오기 실패", e);
            return Optional.empty();
        }
    }

    public Optional<String> searchKoreanNameFromWikipedia(String artistName) {
        try {
            // 1. 아티스트 이름으로 직접 Wikipedia summary API 호출 시도
            Optional<JsonNode> summaryOpt = getWikipediaKoreanSummary(artistName);
            if (summaryOpt.isPresent()) {
                Optional<String> nameOpt = extractKoreanNameFromSummary(summaryOpt.get());
                if (nameOpt.isPresent()) {
                    log.debug("Wikipedia에서 직접 검색 성공: {} -> {}", artistName, nameOpt.get());
                    return nameOpt;
                }
            }

            // 2. Wikipedia Search API로 검색
            // https://ko.wikipedia.org/w/api.php?action=query&list=search&srsearch={query}&format=json
            String encodedQuery = URLEncoder.encode(artistName, StandardCharsets.UTF_8)
                    .replace("+", "%20");
            String searchUrl = String.format(
                    "https://ko.wikipedia.org/w/api.php?action=query&list=search&srsearch=%s&srlimit=5&format=json",
                    encodedQuery
            );

            ResponseEntity<String> response = restTemplate.getForEntity(searchUrl, String.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.debug("Wikipedia 검색 API 응답 실패: name={}, status={}", artistName, response.getStatusCode());
                return Optional.empty();
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode searchResults = root.path("query").path("search");

            if (!searchResults.isArray() || searchResults.isEmpty()) {
                log.debug("Wikipedia 검색 결과 없음: {}", artistName);
                return Optional.empty();
            }

            // 검색 결과들을 순회하며 유효한 제목 찾기
            // snippet과 title을 확인하여 아티스트와 관련이 있는지 검증
            for (JsonNode result : searchResults) {
                String title = result.path("title").asText();
                if (title == null || title.isBlank()) {
                    continue;
                }

                // snippet 확인 (아티스트 관련 키워드가 있는지)
                String snippet = result.path("snippet").asText("");
                String lowerSnippet = snippet.toLowerCase();
                String lowerArtistName = artistName.toLowerCase();
                
                // snippet에 아티스트 이름이 포함되어 있거나, 음악 관련 키워드가 있는지 확인
                boolean isRelevant = lowerSnippet.contains(lowerArtistName) ||
                        lowerSnippet.contains("가수") || lowerSnippet.contains("음악") ||
                        lowerSnippet.contains("singer") || lowerSnippet.contains("musician") ||
                        lowerSnippet.contains("artist") || lowerSnippet.contains("group");
                
                // 제목 자체가 아티스트 이름과 유사한지 확인
                String lowerTitle = title.toLowerCase();
                boolean titleMatches = lowerTitle.contains(lowerArtistName) || 
                        lowerArtistName.contains(lowerTitle.split("\\s+")[0]); // 첫 단어 매칭

                // 관련성이 낮으면 스킵 (너무 많은 결과를 필터링하지 않도록 완화)
                if (!isRelevant && !titleMatches && searchResults.size() > 1) {
                    log.debug("Wikipedia 검색 결과 관련성 낮음, 스킵: title={}, snippet={}", title, snippet);
                    continue;
                }

                // 각 검색 결과의 제목으로 summary 가져오기 시도
                summaryOpt = getWikipediaKoreanSummary(title);
                if (summaryOpt.isPresent()) {
                    Optional<String> nameOpt = extractKoreanNameFromSummary(summaryOpt.get());
                    if (nameOpt.isPresent()) {
                        log.debug("Wikipedia 검색 후 summary에서 이름 추출: {} -> {} (제목: {})", 
                                artistName, nameOpt.get(), title);
                        return nameOpt;
                    }
                    // summary에서 추출 실패하면 제목 사용
                    log.debug("Wikipedia 검색 결과 제목 사용: {} -> {} (제목: {})", 
                            artistName, title, title);
                    return Optional.of(title);
                } else {
                    // 404 등으로 summary를 가져오지 못한 경우, 다음 결과 시도
                    log.debug("Wikipedia 검색 결과 summary 가져오기 실패, 다음 결과 시도: title={}", title);
                }
            }

            // 모든 검색 결과를 시도했지만 실패한 경우
            log.debug("Wikipedia 검색 결과 모두 실패: artistName={}, 검색 결과 수={}", artistName, searchResults.size());

            return Optional.empty();

        } catch (org.springframework.web.client.HttpClientErrorException e) {
            log.warn("Wikipedia 검색 API HTTP 에러: name={}, status={}", artistName, e.getStatusCode());
            return Optional.empty();
        } catch (Exception e) {
            log.warn("Wikipedia에서 직접 한국어 이름 검색 실패: {}", artistName, e);
            return Optional.empty();
        }
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
