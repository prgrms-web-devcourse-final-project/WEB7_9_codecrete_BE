package com.back.web7_9_codecrete_be.global.maniadb;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ManiaDBClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String MANIADB_BASE = "http://www.maniadb.com";
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36";

    /**
     * ManiaDB에서 아티스트 본명 조회
     * @param artistName 아티스트 이름 (활동명)
     * @param nameKo 한국어 이름 (있으면 더 정확한 검색 가능)
     * @return 본명 (없으면 empty)
     */
    public Optional<String> getRealName(String artistName, String nameKo) {
        try {
            // 한국어 이름 우선 사용, 없으면 활동명 사용
            String searchName = (nameKo != null && !nameKo.isBlank()) ? nameKo : artistName;
            
            // 1. 검색 페이지에서 아티스트 페이지 링크 찾기
            Optional<String> artistUrlOpt = searchArtistUrl(searchName);
            if (artistUrlOpt.isEmpty()) {
                log.debug("ManiaDB에서 아티스트 검색 실패: name={}", searchName);
                return Optional.empty();
            }
            
            String artistUrl = artistUrlOpt.get();
            
            // 2. 아티스트 상세 페이지에서 본명 추출
            Optional<String> realNameOpt = extractRealNameFromArtistPage(artistUrl);
            if (realNameOpt.isPresent()) {
                String realName = realNameOpt.get();
                if (realName != null && !realName.isBlank()) {
                    log.info("ManiaDB에서 본명 수집 성공: artistName={}, realName={}", artistName, realName);
                    return Optional.of(realName);
                }
            }
            
            log.debug("ManiaDB에서 본명을 찾을 수 없음: artistName={}, url={}", artistName, artistUrl);
            return Optional.empty();
            
        } catch (Exception e) {
            log.warn("ManiaDB 본명 조회 실패: artistName={}", artistName, e);
            return Optional.empty();
        }
    }
    
    /**
     * ManiaDB 검색 페이지에서 아티스트 URL 찾기
     */
    private Optional<String> searchArtistUrl(String searchName) {
        try {
            String encodedName = URLEncoder.encode(searchName, StandardCharsets.UTF_8);
            String searchUrl = MANIADB_BASE + "/search/" + encodedName + "/?sr=artist";
            
            Document doc = Jsoup.connect(searchUrl)
                    .userAgent(USER_AGENT)
                    .timeout(10000)
                    .get();
            
            // 검색 결과에서 첫 번째 아티스트 링크 찾기
            Elements artistLinks = doc.select("div.search_result a[href^=/artist/]");
            if (!artistLinks.isEmpty()) {
                String href = artistLinks.first().attr("href");
                if (href != null && !href.isBlank()) {
                    String fullUrl = href.startsWith("http") ? href : MANIADB_BASE + href;
                    log.debug("ManiaDB 아티스트 URL 찾음: {}", fullUrl);
                    return Optional.of(fullUrl);
                }
            }
            
            return Optional.empty();
        } catch (Exception e) {
            log.debug("ManiaDB 검색 실패: searchName={}", searchName, e);
            return Optional.empty();
        }
    }
    
    /**
     * 아티스트 상세 페이지에서 본명 추출
     */
    private Optional<String> extractRealNameFromArtistPage(String artistUrl) {
        try {
            Document doc = Jsoup.connect(artistUrl)
                    .userAgent(USER_AGENT)
                    .timeout(10000)
                    .get();
            
            // 본명 정보는 보통 "본명" 또는 "실명" 라벨과 함께 표시됨
            // 다양한 패턴 시도
            
            // 패턴 1: <dt>본명</dt><dd>홍길동</dd>
            Elements dtElements = doc.select("dt");
            for (Element dt : dtElements) {
                String label = dt.text().trim();
                if (label.contains("본명") || label.contains("실명")) {
                    Element dd = dt.nextElementSibling();
                    if (dd != null && dd.tagName().equals("dd")) {
                        String realName = dd.text().trim();
                        if (realName != null && !realName.isBlank() && !realName.equals("-")) {
                            return Optional.of(realName);
                        }
                    }
                }
            }
            
            // 패턴 2: <span class="label">본명</span> 다음의 텍스트
            Elements labelSpans = doc.select("span.label, span.info_label");
            for (Element span : labelSpans) {
                String label = span.text().trim();
                if (label.contains("본명") || label.contains("실명")) {
                    Element parent = span.parent();
                    if (parent != null) {
                        String text = parent.text();
                        // "본명: 홍길동" 형식에서 추출
                        String[] parts = text.split("[:：]");
                        if (parts.length > 1) {
                            String realName = parts[1].trim();
                            if (realName != null && !realName.isBlank() && !realName.equals("-")) {
                                return Optional.of(realName);
                            }
                        }
                    }
                }
            }
            
            // 패턴 3: 테이블에서 본명 찾기
            Elements tables = doc.select("table.info_table, table.artist_info");
            for (Element table : tables) {
                Elements rows = table.select("tr");
                for (Element row : rows) {
                    Elements ths = row.select("th");
                    for (Element th : ths) {
                        String label = th.text().trim();
                        if (label.contains("본명") || label.contains("실명")) {
                            Element td = row.selectFirst("td");
                            if (td != null) {
                                String realName = td.text().trim();
                                if (realName != null && !realName.isBlank() && !realName.equals("-")) {
                                    return Optional.of(realName);
                                }
                            }
                        }
                    }
                }
            }
            
            return Optional.empty();
        } catch (Exception e) {
            log.debug("ManiaDB 아티스트 페이지 파싱 실패: url={}", artistUrl, e);
            return Optional.empty();
        }
    }
}

