package com.back.web7_9_codecrete_be.global.flo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class FloClient {

    private static final String FLO_API_BASE = "https://www.music-flo.com/api";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // FLO에서 아티스트 정보를 검색하여 한국어 이름 가져옴
    public Optional<ArtistInfo> searchArtist(String artistName) {
        try {
            String encodedName = URLEncoder.encode(artistName, StandardCharsets.UTF_8);
            String url = String.format(
                    "%s/search/v2/search/integration?keyword=%s",
                    FLO_API_BASE, encodedName
            );

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            headers.set("Referer", "https://www.music-flo.com/");
            headers.set("Origin", "https://www.music-flo.com");
            // FLO 웹에서 사용하는 헤더들
            headers.set("x-gm-app-name", "FLO_WEB");
            headers.set("Accept", "application/json");

            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.debug("FLO 검색 API 응답 실패: name={}, status={}", artistName, response.getStatusCode());
                return Optional.empty();
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode data = root.path("data");
            if (data.isMissingNode()) {
                log.debug("FLO 응답에 data 필드 없음: name={}", artistName);
                return Optional.empty();
            }

            JsonNode list = data.path("list");
            if (!list.isArray() || list.isEmpty()) {
                log.debug("FLO 응답에 list 배열 없음: name={}", artistName);
                return Optional.empty();
            }

            // 일반적인 FLO 구조: data.list[] 중 type == "ARTIST" 인 섹션에서 실제 아티스트 목록을 찾는다.
            for (JsonNode section : list) {
                String type = section.path("type").asText();
                if (!"ARTIST".equalsIgnoreCase(type)) {
                    continue;
                }

                JsonNode artists = section.path("list");
                if (!artists.isArray() || artists.isEmpty()) {
                    continue;
                }

                JsonNode first = artists.get(0);

                // 필드 이름은 FLO 변경에 따라 달라질 수 있으므로, 여러 후보를 순서대로 시도
                String nameKo = first.path("name").asText(null);
                if (nameKo == null || nameKo.isBlank()) {
                    nameKo = first.path("artistName").asText(null);
                }
                if (nameKo == null || nameKo.isBlank()) {
                    nameKo = first.path("title").asText(null);
                }

                String groupName = first.path("teamName").asText(null);

                if (nameKo == null || nameKo.isBlank()) {
                    log.debug("FLO 아티스트 이름 없음: originalName={}", artistName);
                    return Optional.empty();
                }

                log.debug("FLO 아티스트 정보 파싱 성공: originalName={}, nameKo={}, group={}",
                        artistName, nameKo, groupName);
                return Optional.of(new ArtistInfo(nameKo, groupName));
            }

            log.debug("FLO 응답에 ARTIST 섹션 없음: name={}", artistName);
            return Optional.empty();

        } catch (HttpClientErrorException.NotFound e) {
            // FLO는 공개 API가 아니라 404가 날 수 있음
            log.debug("FLO API 404(NotFound): name={}", artistName);
            return Optional.empty();
        } catch (Exception e) {
            log.debug("FLO 검색 실패: name={}, error={}", artistName, e.getMessage());
            return Optional.empty();
        }
    }

    @Getter
    public static class ArtistInfo {
        private final String nameKo;
        private final String artistGroup;

        public ArtistInfo(String nameKo, String artistGroup) {
            this.nameKo = nameKo;
            this.artistGroup = artistGroup;
        }
    }
}



