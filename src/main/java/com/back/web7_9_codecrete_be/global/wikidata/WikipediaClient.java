package com.back.web7_9_codecrete_be.global.wikidata;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class WikipediaClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String WIKIPEDIA_KO_API = "https://ko.wikipedia.org/api/rest_v1/page/summary/";

    /**
     * Entity에서 Wikipedia 한국어 페이지 제목 가져오기
     */
    public Optional<String> getWikipediaKoreanTitle(JsonNode entity) {
        try {
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

    /**
     * Wikipedia 한국어 summary 가져오기
     */
    public Optional<JsonNode> getWikipediaKoreanSummary(String title) {
        try {
            String encodedTitle = URLEncoder.encode(title, StandardCharsets.UTF_8)
                    .replace("+", "%20");
            String url = WIKIPEDIA_KO_API + encodedTitle;
            
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                int statusCode = response.getStatusCode().value();
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

    /**
     * Wikipedia summary에서 한국어 이름 추출
     */
    public Optional<String> extractKoreanNameFromSummary(JsonNode summary) {
        try {
            JsonNode title = summary.path("title");
            if (!title.isMissingNode() && !title.asText().isBlank()) {
                String titleText = title.asText().trim();
                String cleanTitle = titleText.replaceAll("\\([^)]*\\)", "").trim();
                if (!cleanTitle.isBlank()) {
                    return Optional.of(cleanTitle);
                }
                return Optional.of(titleText);
            }
            
            JsonNode extract = summary.path("extract");
            if (!extract.isMissingNode() && !extract.asText().isBlank()) {
                String extractText = extract.asText();
                String firstSentence = extractText.split("[。\\.]")[0].trim();
                if (firstSentence.length() > 0 && firstSentence.length() <= 20) {
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

    /**
     * Entity에서 Wikipedia를 통해 한국어 이름 가져오기
     */
    public Optional<String> getKoreanNameFromWikipedia(JsonNode entity) {
        try {
            Optional<String> wikiTitleOpt = getWikipediaKoreanTitle(entity);
            if (wikiTitleOpt.isEmpty()) {
                log.debug("Wikipedia 한국어 페이지 제목 없음");
                return Optional.empty();
            }
            
            String wikiTitle = wikiTitleOpt.get();
            log.debug("Wikipedia 한국어 페이지 제목: {}", wikiTitle);
            
            Optional<JsonNode> summaryOpt = getWikipediaKoreanSummary(wikiTitle);
            if (summaryOpt.isEmpty()) {
                log.debug("Wikipedia 한국어 summary 없음: {}", wikiTitle);
                return Optional.of(wikiTitle);
            }
            
            JsonNode summary = summaryOpt.get();
            
            Optional<String> nameOpt = extractKoreanNameFromSummary(summary);
            if (nameOpt.isPresent()) {
                log.debug("Wikipedia summary에서 한국어 이름 추출: {}", nameOpt.get());
                return nameOpt;
            }
            
            log.debug("Wikipedia summary에서 이름 추출 실패, 제목 사용: {}", wikiTitle);
            return Optional.of(wikiTitle);
            
        } catch (Exception e) {
            log.warn("Wikipedia에서 한국어 이름 가져오기 실패", e);
            return Optional.empty();
        }
    }

    /**
     * 아티스트 이름으로 직접 Wikipedia에서 한국어 이름 검색
     */
    public Optional<String> searchKoreanNameFromWikipedia(String artistName) {
        try {
            Optional<JsonNode> summaryOpt = getWikipediaKoreanSummary(artistName);
            if (summaryOpt.isPresent()) {
                Optional<String> nameOpt = extractKoreanNameFromSummary(summaryOpt.get());
                if (nameOpt.isPresent()) {
                    log.debug("Wikipedia에서 직접 검색 성공: {} -> {}", artistName, nameOpt.get());
                    return nameOpt;
                }
            }

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

            for (JsonNode result : searchResults) {
                String title = result.path("title").asText();
                if (title == null || title.isBlank()) {
                    continue;
                }

                String snippet = result.path("snippet").asText("");
                String lowerSnippet = snippet.toLowerCase();
                String lowerArtistName = artistName.toLowerCase();
                
                boolean isRelevant = lowerSnippet.contains(lowerArtistName) ||
                        lowerSnippet.contains("가수") || lowerSnippet.contains("음악") ||
                        lowerSnippet.contains("singer") || lowerSnippet.contains("musician") ||
                        lowerSnippet.contains("artist") || lowerSnippet.contains("group");
                
                String lowerTitle = title.toLowerCase();
                boolean titleMatches = lowerTitle.contains(lowerArtistName) || 
                        lowerArtistName.contains(lowerTitle.split("\\s+")[0]);

                if (!isRelevant && !titleMatches && searchResults.size() > 1) {
                    log.debug("Wikipedia 검색 결과 관련성 낮음, 스킵: title={}, snippet={}", title, snippet);
                    continue;
                }

                summaryOpt = getWikipediaKoreanSummary(title);
                if (summaryOpt.isPresent()) {
                    Optional<String> nameOpt = extractKoreanNameFromSummary(summaryOpt.get());
                    if (nameOpt.isPresent()) {
                        log.debug("Wikipedia 검색 후 summary에서 이름 추출: {} -> {} (제목: {})", 
                                artistName, nameOpt.get(), title);
                        return nameOpt;
                    }
                    log.debug("Wikipedia 검색 결과 제목 사용: {} -> {} (제목: {})", 
                            artistName, title, title);
                    return Optional.of(title);
                } else {
                    log.debug("Wikipedia 검색 결과 summary 가져오기 실패, 다음 결과 시도: title={}", title);
                }
            }

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
}

