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
// 검색 관련 기능
public class MusicBrainzSearchClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final MusicBrainzEntityClient entityClient;

    private static final String MUSICBRAINZ_API_BASE = "https://musicbrainz.org/ws/2";
    private static final String USER_AGENT = "CodecreteBE/1.0 (Educational Project; +https://github.com/your-repo)";

    // 아티스트 이름으로 MusicBrainz에서 아티스트 정보 검색
    public Optional<MusicBrainzEntityClient.ArtistInfo> searchArtist(String artistName) {
        String[] queryFormats = {
                artistName,
                "artist:" + artistName,
                "\"" + artistName + "\"",
                "alias:" + artistName
        };

        String normalizedArtistName = entityClient.normalizeName(artistName);

        for (String query : queryFormats) {
            try {
                String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
                String url = String.format(
                        "%s/artist/?query=%s&fmt=json&limit=5&inc=aliases+artist-rels",
                        MUSICBRAINZ_API_BASE, encodedQuery
                );

                HttpHeaders headers = new HttpHeaders();
                headers.set("User-Agent", USER_AGENT);
                headers.set("Accept", "application/json");
                HttpEntity<String> entity = new HttpEntity<>(headers);

                ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

                if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                    int statusCode = response.getStatusCode().value();
                    if (statusCode == 503) {
                        log.debug("MusicBrainz 서버 일시적 사용 불가: name={}, 쿼리={}, status=503", artistName, query);
                    } else {
                        log.debug("MusicBrainz 검색 API 응답 실패: name={}, 쿼리={}, status={}", artistName, query, statusCode);
                    }
                    continue;
                }

                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode artists = root.path("artists");

                if (!artists.isArray() || artists.isEmpty()) {
                    log.debug("MusicBrainz 검색 결과 없음: 검색어={}, 쿼리={}", artistName, query);
                    continue;
                }

                String normalizedArtistNameForComparison = entityClient.normalizeNameForComparison(artistName);

                for (JsonNode artist : artists) {
                    String mbName = artist.path("name").asText();

                    if (mbName == null || mbName.isBlank()) {
                        continue;
                    }

                    if (entityClient.isExcludedEntity(mbName)) {
                        log.debug("MusicBrainz 검색 결과 제외 (소속사/프로그램 등): name={}, 검색어={}", 
                                mbName, artistName);
                        continue;
                    }

                    String normalizedMbName = entityClient.normalizeName(mbName);
                    String normalizedMbNameForComparison = entityClient.normalizeNameForComparison(mbName);

                    if (normalizedMbName.equals(normalizedArtistName)) {
                        log.debug("MusicBrainz name 완전 일치 발견: 검색어={}, 일치한 name={}, 쿼리={}", 
                                artistName, mbName, query);
                        return entityClient.parseArtistInfo(artist);
                    }

                    if (normalizedMbNameForComparison.equals(normalizedArtistNameForComparison)) {
                        log.debug("MusicBrainz name 하이픈 제거 후 일치 발견: 검색어={}, 일치한 name={}, 쿼리={}", 
                                artistName, mbName, query);
                        return entityClient.parseArtistInfo(artist);
                    }

                    if (normalizedMbName.startsWith(normalizedArtistName + " ") || 
                        normalizedMbName.startsWith(normalizedArtistName + "(")) {
                        log.debug("MusicBrainz name 시작 일치 발견: 검색어={}, 일치한 name={}, 쿼리={}", 
                                artistName, mbName, query);
                        return entityClient.parseArtistInfo(artist);
                    }

                    if (entityClient.checkAliasesMatch(artist, normalizedArtistName)) {
                        log.debug("MusicBrainz aliases 일치 발견: 검색어={}, name={}, 쿼리={}", 
                                artistName, mbName, query);
                        return entityClient.parseArtistInfo(artist);
                    }
                }

                JsonNode bestMatch = null;
                String bestMatchName = null;
                for (JsonNode artist : artists) {
                    String mbName = artist.path("name").asText();
                    if (mbName != null && !mbName.isBlank()) {
                        if (entityClient.isExcludedEntity(mbName)) {
                            log.debug("MusicBrainz 검색 결과 제외 (소속사/프로그램 등): name={}, 검색어={}", 
                                    mbName, artistName);
                            continue;
                        }

                        if (bestMatch == null) {
                            bestMatch = artist;
                            bestMatchName = mbName;
                        }
                    }
                }

                if (bestMatch != null) {
                    log.debug("MusicBrainz name/aliases 정확 일치 없음, 첫 번째 결과 사용: 검색어={}, 선택된 name={}, 정규화된 검색어={}, 정규화된 선택된 name={}, 쿼리={}", 
                            artistName, bestMatchName, normalizedArtistName, entityClient.normalizeName(bestMatchName), query);
                    return entityClient.parseArtistInfo(bestMatch);
                }

            } catch (org.springframework.web.client.HttpServerErrorException e) {
                int statusCode = e.getStatusCode().value();
                if (statusCode == 503) {
                    log.debug("MusicBrainz 서버 일시적 사용 불가: name={}, 쿼리={}, status=503", artistName, query);
                } else {
                    log.debug("MusicBrainz 검색 서버 에러: name={}, 쿼리={}, status={}", artistName, query, statusCode);
                }
                continue;
            } catch (org.springframework.web.client.ResourceAccessException e) {
                log.debug("MusicBrainz 네트워크 에러: name={}, 쿼리={}, error={}", 
                        artistName, query, e.getMessage());
                continue;
            } catch (Exception e) {
                log.debug("MusicBrainz 검색 실패: name={}, 쿼리={}, error={}", artistName, query, e.getMessage());
                continue;
            }
        }

        log.warn("MusicBrainz 모든 쿼리 형식 시도 후 검색 결과 없음: 검색어={}", artistName);
        return Optional.empty();
    }

    // Spotify URL로 MusicBrainz ID 찾기
    public Optional<String> searchMbidBySpotifyUrl(String spotifyId) {
        try {
            String spotifyUrl = String.format("https://open.spotify.com/artist/%s", spotifyId);
            String query = String.format("url:\"%s\"", spotifyUrl);
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            
            String url = String.format(
                    "%s/artist/?query=%s&fmt=json&limit=1",
                    MUSICBRAINZ_API_BASE, encodedQuery
            );

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", USER_AGENT);
            headers.set("Accept", "application/json");
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.debug("MusicBrainz Spotify URL 검색 API 응답 실패: spotifyId={}, status={}", 
                        spotifyId, response.getStatusCode());
                return Optional.empty();
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode artists = root.path("artists");

            if (!artists.isArray() || artists.isEmpty()) {
                log.debug("MusicBrainz Spotify URL 검색 결과 없음: spotifyId={}", spotifyId);
                return Optional.empty();
            }

            JsonNode firstArtist = artists.get(0);
            String mbid = firstArtist.path("id").asText();
            
            if (mbid != null && !mbid.isBlank()) {
                log.debug("Spotify URL로 MusicBrainz ID 찾음: spotifyId={}, mbid={}", spotifyId, mbid);
                return Optional.of(mbid);
            }

            return Optional.empty();

        } catch (org.springframework.web.client.HttpServerErrorException e) {
            int statusCode = e.getStatusCode().value();
            if (statusCode == 503) {
                log.debug("MusicBrainz 서버 일시적 사용 불가: spotifyId={}, status=503", spotifyId);
            } else {
                log.warn("MusicBrainz Spotify URL 검색 서버 에러: spotifyId={}, status={}", spotifyId, statusCode);
            }
            return Optional.empty();
        } catch (org.springframework.web.client.ResourceAccessException e) {
            log.debug("MusicBrainz 네트워크 에러: spotifyId={}, error={}", spotifyId, e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            log.warn("MusicBrainz Spotify URL 검색 실패: spotifyId={}", spotifyId, e);
            return Optional.empty();
        }
    }
}

