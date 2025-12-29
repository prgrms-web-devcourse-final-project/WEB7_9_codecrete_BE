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
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class MusicBrainzClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String MUSICBRAINZ_API_BASE = "https://musicbrainz.org/ws/2";
    private static final String USER_AGENT = "CodecreteBE/1.0 (Educational Project; +https://github.com/your-repo)";

    // 소속사, 출연 프로그램, 레이블 등 아티스트가 아닌 엔티티 필터링 키워드
    private static final List<String> EXCLUDED_KEYWORDS = Arrays.asList(
            // 소속사/레이블
            "smtown", "sm entertainment", "yg entertainment", "jyp entertainment", "jyp",
            "hybe", "big hit", "bighit", "cube entertainment", "cube",
            "fnc entertainment", "fnc", "dsp media", "dsp",
            "starship entertainment", "starship", "rbw", "rainbow bridge world",
            "wm entertainment", "wm", "pledis entertainment", "pledis",
            "source music", "wakeone", "cj enm", "mnet", "kbs", "mbc", "sbs",
            "loen entertainment", "loen", "kakao m", "genie music", "genie",
            "melon", "bugs", "flo", "vibe",
            // 출연 프로그램
            "produce 101", "produce", "show me the money", "k-pop star", "kpop star",
            "superstar k", "the voice", "masked singer", "king of masked singer",
            "sugar man", "immortal songs", "fantastic duo", "hidden singer",
            "i can see your voice", "queendom", "kingdom", "girls planet",
            "boys planet", "unpretty rapstar", "good girl", "show me the money",
            // 기타
            "music bank", "music core", "inkigayo", "show champion", "m countdown",
            "the show", "music show", "award", "festival", "concert"
    );

    // 아티스트 이름으로 MusicBrainz에서 아티스트 정보 검색
    public Optional<ArtistInfo> searchArtist(String artistName) {
        // 여러 쿼리 형식 시도
        String[] queryFormats = {
                artistName,  // 단순 검색어
                "artist:" + artistName,  // artist 필드 지정
                "\"" + artistName + "\"",  // 따옴표 포함
                "alias:" + artistName  // alias 필드 검색
        };

        String normalizedArtistName = normalizeName(artistName);

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

                // 정규화된 이름으로 일치하는 결과 찾기 (대소문자 무시, 하이픈 제거 비교 포함)
                String normalizedArtistNameForComparison = normalizeNameForComparison(artistName);
                
                for (JsonNode artist : artists) {
                    String mbName = artist.path("name").asText();
                    
                    if (mbName == null || mbName.isBlank()) {
                        continue;
                    }
                    
                    // 소속사/출연 프로그램 등 아티스트가 아닌 엔티티 필터링
                    if (isExcludedEntity(mbName)) {
                        log.debug("MusicBrainz 검색 결과 제외 (소속사/프로그램 등): name={}, 검색어={}", 
                                mbName, artistName);
                        continue;
                    }
                    
                    String normalizedMbName = normalizeName(mbName);
                    String normalizedMbNameForComparison = normalizeNameForComparison(mbName);
                    
                    // 1순위: name 필드가 정규화된 비교로 완전 일치하는 경우 (대소문자 무시)
                    if (normalizedMbName.equals(normalizedArtistName)) {
                        log.debug("MusicBrainz name 완전 일치 발견: 검색어={}, 일치한 name={}, 쿼리={}", 
                                artistName, mbName, query);
                        return parseArtistInfo(artist);
                    }
                    
                    // 2순위: 하이픈 제거 후 비교 (예: "GDRAGON"과 "G-DRAGON" 매칭)
                    if (normalizedMbNameForComparison.equals(normalizedArtistNameForComparison)) {
                        log.debug("MusicBrainz name 하이픈 제거 후 일치 발견: 검색어={}, 일치한 name={}, 쿼리={}", 
                                artistName, mbName, query);
                        return parseArtistInfo(artist);
                    }
                    
                    // 3순위: name 필드가 정규화된 검색어로 시작하는 경우 (예: "rosé"와 "rosé (blackpink)")
                    if (normalizedMbName.startsWith(normalizedArtistName + " ") || 
                        normalizedMbName.startsWith(normalizedArtistName + "(")) {
                        log.debug("MusicBrainz name 시작 일치 발견: 검색어={}, 일치한 name={}, 쿼리={}", 
                                artistName, mbName, query);
                        return parseArtistInfo(artist);
                    }
                    
                    // 4순위: aliases에 정규화된 검색어가 일치하는 경우
                    if (checkAliasesMatch(artist, normalizedArtistName)) {
                        log.debug("MusicBrainz aliases 일치 발견: 검색어={}, name={}, 쿼리={}", 
                                artistName, mbName, query);
                        return parseArtistInfo(artist);
                    }
                }

                // 일치하는 것이 없으면 정규화된 비교로 가장 유사한 결과 찾기
                JsonNode bestMatch = null;
                String bestMatchName = null;
                for (JsonNode artist : artists) {
                    String mbName = artist.path("name").asText();
                    if (mbName != null && !mbName.isBlank()) {
                        // 소속사/출연 프로그램 등 아티스트가 아닌 엔티티 필터링
                        if (isExcludedEntity(mbName)) {
                            log.debug("MusicBrainz 검색 결과 제외 (소속사/프로그램 등): name={}, 검색어={}", 
                                    mbName, artistName);
                            continue;
                        }
                        
                        // 정규화된 이름이 완전히 일치하지 않더라도, 첫 번째 결과를 사용
                        // (이미 정규화된 비교를 시도했으므로, 첫 번째가 가장 관련성 높은 결과)
                        if (bestMatch == null) {
                            bestMatch = artist;
                            bestMatchName = mbName;
                        }
                    }
                }
                
                if (bestMatch != null) {
                    log.debug("MusicBrainz name/aliases 정확 일치 없음, 첫 번째 결과 사용: 검색어={}, 선택된 name={}, 정규화된 검색어={}, 정규화된 선택된 name={}, 쿼리={}", 
                            artistName, bestMatchName, normalizedArtistName, normalizeName(bestMatchName), query);
                    return parseArtistInfo(bestMatch);
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

    // Spotify URL로 MusicBrainz ID 찾기
    public Optional<String> searchMbidBySpotifyUrl(String spotifyId) {
        try {
            String spotifyUrl = String.format("https://open.spotify.com/artist/%s", spotifyId);
            String encodedUrl = URLEncoder.encode(spotifyUrl, StandardCharsets.UTF_8);
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

    private Optional<ArtistInfo> parseArtistInfo(JsonNode artist) {
        try {
            String mbid = artist.path("id").asText();
            String name = artist.path("name").asText();
            String nameKo = extractKoreanName(artist);
            String artistGroup = extractGroupName(artist);
            String artistType = extractArtistType(artist);

            return Optional.of(new ArtistInfo(mbid, name, nameKo, artistGroup, artistType));

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

    // 소속 그룹 이름 추출 (relations에서 type="member of band" AND artist.type="Group"만 사용)
    private String extractGroupName(JsonNode artist) {
        JsonNode relations = artist.path("relations");
        if (!relations.isArray() || relations.isEmpty()) {
            return null;
        }
        
        // "member of band" 타입이면서 relation.artist.type == "Group"인 경우만 사용
        for (JsonNode relation : relations) {
            String type = relation.path("type").asText();
            
            if ("member of band".equals(type)) {
                JsonNode group = relation.path("artist");
                if (!group.isMissingNode()) {
                    // relation.artist.type == "Group" 체크
                    String groupType = group.path("type").asText();
                    if (!"Group".equals(groupType)) {
                        log.debug("relation.artist.type이 Group이 아니어서 제외: type={}", groupType);
                        continue;
                    }
                    
                    String groupName = group.path("name").asText();
                    if (groupName != null && !groupName.isBlank()) {
                        // 소속사, 출연 프로그램, 이벤트성 그룹 필터링
                        if (isExcludedGroupName(groupName)) {
                            log.debug("소속 그룹 제외 (소속사/프로그램/이벤트): groupName={}", groupName);
                            continue;
                        }
                        
                        log.debug("소속 그룹 추출 성공: groupName={}, relation.type={}, artist.type={}", 
                                groupName, type, groupType);
                        return groupName;
                    }
                }
            }
        }
        
        return null;
    }
    
    /**
     * 그룹명이 소속사, 출연 프로그램, 이벤트성 그룹인지 확인
     * @param groupName 그룹명
     * @return 제외해야 할 그룹명이면 true
     */
    private boolean isExcludedGroupName(String groupName) {
        if (groupName == null || groupName.isBlank()) {
            return false;
        }
        
        String normalizedName = normalizeName(groupName);
        String lowerName = normalizedName.toLowerCase();
        
        // 제외 키워드 목록과 비교
        for (String keyword : EXCLUDED_KEYWORDS) {
            if (normalizedName.equals(keyword) || 
                lowerName.contains(keyword)) {
                return true;
            }
        }
        
        // 추가 이벤트성 그룹 키워드
        String[] eventKeywords = {
            "collaboration", "collab", "special stage", "special unit",
            "project group", "temporary", "one-time", "event",
            "collaboration stage", "collab stage", "special collaboration"
        };
        for (String keyword : eventKeywords) {
            if (lowerName.contains(keyword)) {
                return true;
            }
        }
        
        // 숫자만 있는 경우
        if (normalizedName.matches("^\\d+$")) {
            return true;
        }
        
        return false;
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

    // aliases에서 검색어와 일치하는 것이 있는지 확인 (정규화된 비교, 대소문자 무시, 하이픈 제거 비교 포함)
    private boolean checkAliasesMatch(JsonNode artist, String normalizedSearchName) {
        try {
            JsonNode aliases = artist.path("aliases");
            if (aliases.isArray()) {
                String normalizedSearchNameForComparison = normalizeNameForComparison(normalizedSearchName);
                
                for (JsonNode alias : aliases) {
                    String aliasName = alias.path("name").asText();
                    if (aliasName != null) {
                        String normalizedAliasName = normalizeName(aliasName);
                        String normalizedAliasNameForComparison = normalizeNameForComparison(aliasName);
                        
                        // 하이픈 포함 비교
                        if (normalizedAliasName.equals(normalizedSearchName)) {
                            return true;
                        }
                        
                        // 하이픈 제거 후 비교
                        if (normalizedAliasNameForComparison.equals(normalizedSearchNameForComparison)) {
                            return true;
                        }
                    }
                }
            }
            return false;
        } catch (Exception e) {
            log.debug("Aliases 확인 실패", e);
            return false;
        }
    }

    // 이름 정규화: 대소문자 통일, 앞뒤 공백 제거, 하이픈/대시 문자 정규화 (괄호는 보존)
    private String normalizeName(String name) {
        if (name == null) {
            return "";
        }
        return name.trim()
                .toLowerCase()
                // 다양한 하이픈/대시 문자를 일반 하이픈(-)으로 통일
                .replaceAll("[‐‑‒–—―−]", "-")
                // 연속된 공백을 하나로 통일
                .replaceAll("\\s+", " ")
                .trim();
    }

    // 이름 비교용 정규화: 하이픈도 제거하여 비교 (예: "GDRAGON"과 "G-DRAGON" 매칭)
    private String normalizeNameForComparison(String name) {
        if (name == null) {
            return "";
        }
        return normalizeName(name)
                // 하이픈 제거 (예: "g-dragon" → "gdragon")
                .replaceAll("-", "");
    }

    /**
     * 소속사, 출연 프로그램, 레이블 등 아티스트가 아닌 엔티티인지 확인
     * @param name MusicBrainz에서 반환된 이름
     * @return 제외해야 할 엔티티면 true
     */
    private boolean isExcludedEntity(String name) {
        if (name == null || name.isBlank()) {
            return false;
        }
        
        String normalizedName = normalizeName(name);
        
        // 제외 키워드 목록과 비교
        for (String keyword : EXCLUDED_KEYWORDS) {
            // 정확히 일치하거나, 이름이 키워드로 시작하는 경우 제외
            if (normalizedName.equals(keyword) || 
                normalizedName.startsWith(keyword + " ") ||
                normalizedName.startsWith(keyword + "(") ||
                normalizedName.startsWith(keyword + "-")) {
                return true;
            }
        }
        
        // 추가 패턴 체크: 숫자만 있는 경우 (예: "101", "2020" 등)
        if (normalizedName.matches("^\\d+$")) {
            return true;
        }
        
        return false;
    }

    public static class ArtistInfo {
        private final String mbid;
        private final String name;
        private final String nameKo;
        private final String artistGroup;
        private final String artistType;

        public ArtistInfo(String mbid, String name, String nameKo, String artistGroup, String artistType) {
            this.mbid = mbid;
            this.name = name;
            this.nameKo = nameKo;
            this.artistGroup = artistGroup;
            this.artistType = artistType;
        }

        public String getMbid() {
            return mbid;
        }

        public String getName() {
            return name;
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
