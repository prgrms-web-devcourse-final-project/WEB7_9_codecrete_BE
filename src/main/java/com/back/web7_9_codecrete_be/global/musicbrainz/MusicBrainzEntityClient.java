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

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
// Entity 조회 및 정보 추출
public class MusicBrainzEntityClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String MUSICBRAINZ_API_BASE = "https://musicbrainz.org/ws/2";
    private static final String USER_AGENT = "CodecreteBE/1.0 (Educational Project; +https://github.com/your-repo)";

    private static final List<String> EXCLUDED_KEYWORDS = Arrays.asList(
            "smtown", "sm entertainment", "yg entertainment", "jyp entertainment", "jyp",
            "hybe", "big hit", "bighit", "cube entertainment", "cube",
            "fnc entertainment", "fnc", "dsp media", "dsp",
            "starship entertainment", "starship", "rbw", "rainbow bridge world",
            "wm entertainment", "wm", "pledis entertainment", "pledis",
            "source music", "wakeone", "cj enm", "mnet", "kbs", "mbc", "sbs",
            "loen entertainment", "loen", "kakao m", "genie music", "genie",
            "melon", "bugs", "flo", "vibe",
            "produce 101", "produce", "show me the money", "k-pop star", "kpop star",
            "superstar k", "the voice", "masked singer", "king of masked singer",
            "sugar man", "immortal songs", "fantastic duo", "hidden singer",
            "i can see your voice", "queendom", "kingdom", "girls planet",
            "boys planet", "unpretty rapstar", "good girl", "show me the money",
            "music bank", "music core", "inkigayo", "show champion", "m countdown",
            "the show", "music show", "award", "festival", "concert"
    );

    /**
     * MusicBrainz MBID로 상세 아티스트 정보 조회
     */
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

    // MusicBrainz MBID로 아티스트 활동 종료 여부 확인
    public Optional<Boolean> isEnded(String mbid) {
        try {
            String url = String.format(
                    "%s/artist/%s?fmt=json",
                    MUSICBRAINZ_API_BASE, mbid
            );

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", USER_AGENT);
            headers.set("Accept", "application/json");
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.debug("MusicBrainz ended 조회 실패: mbid={}, status={}", mbid, response.getStatusCode());
                return Optional.empty();
            }

            JsonNode artist = objectMapper.readTree(response.getBody());
            JsonNode lifeSpan = artist.path("life-span");
            
            if (lifeSpan.isMissingNode()) {
                return Optional.empty();
            }
            
            JsonNode ended = lifeSpan.path("ended");
            if (ended.isMissingNode() || ended.isNull()) {
                return Optional.empty();
            }
            
            boolean isEnded = ended.asBoolean(false);
            return Optional.of(isEnded);

        } catch (org.springframework.web.client.HttpServerErrorException e) {
            int statusCode = e.getStatusCode().value();
            if (statusCode == 503) {
                log.debug("MusicBrainz 서버 일시적 사용 불가: mbid={}, status=503", mbid);
            } else {
                log.debug("MusicBrainz ended 조회 서버 에러: mbid={}, status={}", mbid, statusCode);
            }
            return Optional.empty();
        } catch (org.springframework.web.client.ResourceAccessException e) {
            log.debug("MusicBrainz 네트워크 에러: mbid={}, error={}", mbid, e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            log.debug("MusicBrainz ended 조회 실패: mbid={}", mbid, e);
            return Optional.empty();
        }
    }

    // MusicBrainz MBID로 서브 유닛/프로젝트 그룹 여부 확인
    public Optional<Boolean> isSubunitOrProjectGroup(String mbid) {
        try {
            String url = String.format(
                    "%s/artist/%s?fmt=json&inc=artist-rels",
                    MUSICBRAINZ_API_BASE, mbid
            );

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", USER_AGENT);
            headers.set("Accept", "application/json");
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.debug("MusicBrainz 서브 유닛 조회 API 응답 실패: mbid={}, status={}", mbid, response.getStatusCode());
                return Optional.empty();
            }

            JsonNode artist = objectMapper.readTree(response.getBody());
            
            String disambiguation = artist.path("disambiguation").asText();
            if (disambiguation != null && !disambiguation.isBlank()) {
                String lowerDisambiguation = disambiguation.toLowerCase();
                if (lowerDisambiguation.contains("subunit") ||
                    lowerDisambiguation.contains("sub-group") ||
                    lowerDisambiguation.contains("project group") ||
                    lowerDisambiguation.contains("part of")) {
                    log.info("MusicBrainz 서브 유닛 감지 (disambiguation): mbid={}, disambiguation={}", mbid, disambiguation);
                    return Optional.of(true);
                }
            }
            
            JsonNode relations = artist.path("relations");
            if (relations.isArray() && !relations.isEmpty()) {
                for (JsonNode relation : relations) {
                    String type = relation.path("type").asText();
                    if (type == null || type.isBlank()) {
                        continue;
                    }
                    
                    String lowerType = type.toLowerCase();
                    if (lowerType.contains("member of")) {
                        JsonNode targetArtist = relation.path("artist");
                        if (!targetArtist.isMissingNode()) {
                            String targetType = targetArtist.path("type").asText();
                            if ("Group".equals(targetType)) {
                                log.info("MusicBrainz 서브 유닛 감지 (member of): mbid={}, relation.type={}, targetGroup={}", 
                                        mbid, type, targetArtist.path("name").asText());
                                return Optional.of(true);
                            }
                        }
                    }
                    
                    if (lowerType.contains("subgroup of") || lowerType.equals("subgroup")) {
                        log.info("MusicBrainz 서브 유닛 감지 (subgroup of): mbid={}, relation.type={}", mbid, type);
                        return Optional.of(true);
                    }
                }
            }
            
            return Optional.of(false);
            
        } catch (org.springframework.web.client.HttpServerErrorException e) {
            int statusCode = e.getStatusCode().value();
            if (statusCode == 503) {
                log.debug("MusicBrainz 서버 일시적 사용 불가: mbid={}, status=503", mbid);
            } else {
                log.debug("MusicBrainz 서브 유닛 조회 서버 에러: mbid={}, status={}", mbid, statusCode);
            }
            return Optional.empty();
        } catch (org.springframework.web.client.ResourceAccessException e) {
            log.debug("MusicBrainz 네트워크 에러: mbid={}, error={}", mbid, e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            log.debug("MusicBrainz 서브 유닛 조회 실패: mbid={}", mbid, e);
            return Optional.empty();
        }
    }

    // 아티스트 정보 파싱
    public Optional<ArtistInfo> parseArtistInfo(JsonNode artist) {
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

    // 한국어 이름 추출 (aliases에서 locale="ko"인 것)
    public String extractKoreanName(JsonNode artist) {
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

    /**
     * 소속 그룹 이름 추출
     */
    public String extractGroupName(JsonNode artist) {
        JsonNode relations = artist.path("relations");
        if (!relations.isArray() || relations.isEmpty()) {
            return null;
        }
        
        for (JsonNode relation : relations) {
            String type = relation.path("type").asText();
            
            if ("member of band".equals(type)) {
                JsonNode group = relation.path("artist");
                if (!group.isMissingNode()) {
                    String groupType = group.path("type").asText();
                    if (!"Group".equals(groupType)) {
                        log.debug("relation.artist.type이 Group이 아니어서 제외: type={}", groupType);
                        continue;
                    }
                    
                    String groupName = group.path("name").asText();
                    if (groupName != null && !groupName.isBlank()) {
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

    // 그룹명이 소속사, 출연 프로그램, 이벤트성 그룹인지 확인
    public boolean isExcludedGroupName(String groupName) {
        if (groupName == null || groupName.isBlank()) {
            return false;
        }
        
        String normalizedName = normalizeName(groupName);
        String lowerName = normalizedName.toLowerCase();
        
        for (String keyword : EXCLUDED_KEYWORDS) {
            if (normalizedName.equals(keyword) || 
                lowerName.contains(keyword)) {
                return true;
            }
        }
        
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
        
        if (normalizedName.matches("^\\d+$")) {
            return true;
        }
        
        return false;
    }

    // 아티스트 타입 추출
    public String extractArtistType(JsonNode artist) {
        String type = artist.path("type").asText();
        if ("Group".equals(type) || "Orchestra".equals(type) || "Choir".equals(type)) {
            return "GROUP";
        } else if ("Person".equals(type)) {
            return "SOLO";
        }
        return null;
    }

    public boolean checkAliasesMatch(JsonNode artist, String normalizedSearchName) {
        try {
            JsonNode aliases = artist.path("aliases");
            if (aliases.isArray()) {
                String normalizedSearchNameForComparison = normalizeNameForComparison(normalizedSearchName);
                
                for (JsonNode alias : aliases) {
                    String aliasName = alias.path("name").asText();
                    if (aliasName != null) {
                        String normalizedAliasName = normalizeName(aliasName);
                        String normalizedAliasNameForComparison = normalizeNameForComparison(aliasName);
                        
                        if (normalizedAliasName.equals(normalizedSearchName)) {
                            return true;
                        }
                        
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

    public String normalizeName(String name) {
        if (name == null) {
            return "";
        }
        return name.trim()
                .toLowerCase()
                .replaceAll("[‐‑‒–—―−]", "-")
                .replaceAll("\\s+", " ")
                .trim();
    }

    public String normalizeNameForComparison(String name) {
        if (name == null) {
            return "";
        }
        return normalizeName(name)
                .replaceAll("-", "");
    }

    // 소속사, 출연 프로그램, 레이블 등 아티스트가 아닌 엔티티인지 확인
    public boolean isExcludedEntity(String name) {
        if (name == null || name.isBlank()) {
            return false;
        }
        
        String normalizedName = normalizeName(name);
        
        for (String keyword : EXCLUDED_KEYWORDS) {
            if (normalizedName.equals(keyword) || 
                normalizedName.startsWith(keyword + " ") ||
                normalizedName.startsWith(keyword + "(") ||
                normalizedName.startsWith(keyword + "-")) {
                return true;
            }
        }
        
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

