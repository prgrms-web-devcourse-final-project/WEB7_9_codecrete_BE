package com.back.web7_9_codecrete_be.global.musicbrainz;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class MusicBrainzClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String MUSICBRAINZ_API_BASE = "https://musicbrainz.org/ws/2";
    private static final String USER_AGENT = "CodecreteBE/1.0 (Educational Project; +https://github.com/your-repo)";

    // 아티스트 이름으로 MusicBrainz에서 아티스트 정보 검색
    public Optional<ArtistInfo> searchArtist(String artistName) {
        try {
            String encodedName = URLEncoder.encode(artistName, StandardCharsets.UTF_8);
            String url = String.format(
                    "%s/artist/?query=artist:\"%s\"&fmt=json&limit=5&inc=aliases+artist-rels",
                    MUSICBRAINZ_API_BASE, encodedName
            );

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", USER_AGENT);
            headers.set("Accept", "application/json");
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                int statusCode = response.getStatusCode().value();
                // 503 Service Unavailable은 서버가 일시적으로 사용 불가능한 경우
                if (statusCode == 503) {
                    log.debug("MusicBrainz 서버 일시적 사용 불가: name={}, status=503 (나중에 다시 시도 필요)", artistName);
                } else {
                    log.debug("MusicBrainz 검색 API 응답 실패: name={}, status={}", artistName, statusCode);
                }
                return Optional.empty();
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode artists = root.path("artists");

            if (!artists.isArray() || artists.isEmpty()) {
                log.debug("MusicBrainz 검색 결과 없음: {}", artistName);
                return Optional.empty();
            }

            // 첫 번째 결과 사용 (가장 관련성 높은 결과)
            JsonNode firstArtist = artists.get(0);
            return parseArtistInfo(firstArtist);

        } catch (org.springframework.web.client.HttpServerErrorException e) {
            // 503 Service Unavailable 등 서버 에러 명시적 처리
            int statusCode = e.getStatusCode().value();
            if (statusCode == 503) {
                log.debug("MusicBrainz 서버 일시적 사용 불가: name={}, status=503 (나중에 다시 시도 필요)", artistName);
            } else {
                log.warn("MusicBrainz 검색 서버 에러: name={}, status={}", artistName, statusCode);
            }
            return Optional.empty();
        } catch (org.springframework.web.client.ResourceAccessException e) {
            // Connection reset, timeout 등 네트워크 에러 처리
            log.debug("MusicBrainz 네트워크 에러 (Connection reset/timeout): name={}, error={}", 
                    artistName, e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            log.warn("MusicBrainz 검색 실패: name={}", artistName, e);
            return Optional.empty();
        }
    }

    // MusicBrainz MBID로 상세 아티스트 정보 조회
    public Optional<ArtistInfo> getArtistByMbid(String mbid) {
        try {
            String url = String.format(
                    "%s/artist/%s?fmt=json&inc=aliases+artist-rels",
                    MUSICBRAINZ_API_BASE, mbid
            );

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", USER_AGENT);
            headers.set("Accept", "application/json");
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.debug("MusicBrainz 조회 API 응답 실패: mbid={}, status={}", mbid, response.getStatusCode());
                return Optional.empty();
            }

            JsonNode artist = objectMapper.readTree(response.getBody());
            return parseArtistInfo(artist);

        } catch (org.springframework.web.client.HttpServerErrorException e) {
            int statusCode = e.getStatusCode().value();
            if (statusCode == 503) {
                log.debug("MusicBrainz 서버 일시적 사용 불가: mbid={}, status=503", mbid);
            } else {
                log.warn("MusicBrainz 조회 서버 에러: mbid={}, status={}", mbid, statusCode);
            }
            return Optional.empty();
        } catch (org.springframework.web.client.ResourceAccessException e) {
            log.debug("MusicBrainz 네트워크 에러 (Connection reset/timeout): mbid={}, error={}", 
                    mbid, e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            log.warn("MusicBrainz 조회 실패: mbid={}", mbid, e);
            return Optional.empty();
        }
    }

    private Optional<ArtistInfo> parseArtistInfo(JsonNode artist) {
        try {
            String nameKo = extractKoreanName(artist);
            String artistGroup = extractGroupName(artist);
            String artistType = extractArtistType(artist);

            return Optional.of(new ArtistInfo(nameKo, artistGroup, artistType));

        } catch (Exception e) {
            log.warn("MusicBrainz 아티스트 정보 파싱 실패", e);
            return Optional.empty();
        }
    }

    /**
     * 한국어 이름 추출 (aliases에서 locale="ko"인 것)
     */
    private String extractKoreanName(JsonNode artist) {
        JsonNode aliases = artist.path("aliases");
        if (aliases.isArray()) {
            for (JsonNode alias : aliases) {
                String locale = alias.path("locale").asText();
                if ("ko".equals(locale)) {
                    String name = alias.path("name").asText();
                    if (name != null && !name.isBlank()) {
                        return name;
                    }
                }
            }
        }
        return null;
    }

    // 소속 그룹 이름 추출 (relations에서 type="member of band"인 것)
    private String extractGroupName(JsonNode artist) {
        JsonNode relations = artist.path("relations");
        if (relations.isArray()) {
            for (JsonNode relation : relations) {
                String type = relation.path("type").asText();
                if ("member of band".equals(type) || "member of".equals(type)) {
                    JsonNode group = relation.path("artist");
                    if (!group.isMissingNode()) {
                        String groupName = group.path("name").asText();
                        if (groupName != null && !groupName.isBlank()) {
                            return groupName;
                        }
                    }
                }
            }
        }
        return null;
    }

    // 아티스트 타입 추출 (type 필드: Person -> SOLO, Group -> GROUP)
    private String extractArtistType(JsonNode artist) {
        String type = artist.path("type").asText();
        if ("Group".equals(type) || "Orchestra".equals(type) || "Choir".equals(type)) {
            return "GROUP";
        } else if ("Person".equals(type)) {
            return "SOLO";
        }
        // type이 없거나 다른 경우 null 반환 (기존 값 유지)
        return null;
    }

    public static class ArtistInfo {
        private final String nameKo;
        private final String artistGroup;
        private final String artistType;

        public ArtistInfo(String nameKo, String artistGroup, String artistType) {
            this.nameKo = nameKo;
            this.artistGroup = artistGroup;
            this.artistType = artistType;
        }

        public String getNameKo() {
            return nameKo;
        }

        public String getArtistGroup() {
            return artistGroup;
        }

        public String getArtistType() {
            return artistType;
        }
    }
}

