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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
// 실명 조회 전용
public class MusicBrainzRealNameClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final MusicBrainzEntityClient entityClient;

    private static final String MUSICBRAINZ_API_BASE = "https://musicbrainz.org/ws/2";
    private static final String USER_AGENT = "CodecreteBE/1.0 (Educational Project; +https://github.com/your-repo)";

    // MusicBrainz MBID로 aliases 후보 수집 (type/primary/locale 정보 포함)
    public List<AliasCandidate> collectAliasCandidates(String mbid) {
        List<AliasCandidate> candidates = new ArrayList<>();
        
        try {
            String url = String.format(
                    "%s/artist/%s?fmt=json&inc=aliases",
                    MUSICBRAINZ_API_BASE, mbid
            );

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", USER_AGENT);
            headers.set("Accept", "application/json");
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.debug("MusicBrainz aliases 조회 실패: mbid={}, status={}", mbid, response.getStatusCode());
                return candidates;
            }

            JsonNode artist = objectMapper.readTree(response.getBody());
            JsonNode aliases = artist.path("aliases");
            
            if (!aliases.isArray() || aliases.isEmpty()) {
                return candidates;
            }
            
            for (JsonNode alias : aliases) {
                String aliasName = alias.path("name").asText();
                if (aliasName == null || aliasName.isBlank()) {
                    continue;
                }
                
                String type = alias.path("type").asText();
                boolean isPrimary = alias.path("primary").asBoolean(false);
                String locale = alias.path("locale").asText();
                
                candidates.add(new AliasCandidate(aliasName, type, isPrimary, locale));
            }
        } catch (Exception e) {
            log.warn("MusicBrainz aliases 후보 수집 실패: mbid={}", mbid, e);
        }
        
        return candidates;
    }
    
    // MusicBrainz MBID로 실명 조회
    public Optional<String> getRealNameByMbid(String mbid, String stageName) {
        try {
            Optional<MusicBrainzEntityClient.ArtistInfo> artistInfoOpt = entityClient.getArtistByMbid(mbid);
            if (artistInfoOpt.isEmpty()) {
                log.debug("MusicBrainz에서 아티스트 정보를 찾을 수 없음: mbid={}", mbid);
                return Optional.empty();
            }
            
            String url = String.format(
                    "%s/artist/%s?fmt=json&inc=aliases",
                    MUSICBRAINZ_API_BASE, mbid
            );

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", USER_AGENT);
            headers.set("Accept", "application/json");
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.debug("MusicBrainz aliases 조회 실패: mbid={}, status={}", mbid, response.getStatusCode());
                return Optional.empty();
            }

            JsonNode artist = objectMapper.readTree(response.getBody());
            JsonNode aliases = artist.path("aliases");
            
            if (!aliases.isArray() || aliases.isEmpty()) {
                log.debug("MusicBrainz aliases 없음: mbid={}", mbid);
                return Optional.empty();
            }
            
            String normalizedStageName = entityClient.normalizeNameForComparison(stageName != null ? stageName : "");
            
            for (JsonNode alias : aliases) {
                String aliasName = alias.path("name").asText();
                if (aliasName == null || aliasName.isBlank()) {
                    continue;
                }
                
                String type = alias.path("type").asText();
                if ("Legal name".equals(type) || "Birth name".equals(type)) {
                    log.debug("MusicBrainz에서 legal/birth name 찾음: mbid={}, realName={}, type={}", 
                            mbid, aliasName, type);
                    return Optional.of(aliasName);
                }
                
                String normalizedAliasName = entityClient.normalizeNameForComparison(aliasName);
                if (!normalizedAliasName.equals(normalizedStageName)) {
                    if (aliasName.matches(".*[가-힣].*")) {
                        log.debug("MusicBrainz에서 한글 alias를 실명으로 추정: mbid={}, realName={}", 
                                mbid, aliasName);
                        return Optional.of(aliasName);
                    }
                }
            }
            
            log.debug("MusicBrainz에서 실명을 찾을 수 없음: mbid={}, stageName={}", mbid, stageName);
            return Optional.empty();
            
        } catch (Exception e) {
            log.warn("MusicBrainz MBID로 실명 조회 실패: mbid={}, stageName={}", mbid, stageName, e);
            return Optional.empty();
        }
    }

    // 후보
    public static class AliasCandidate {
        private final String name;
        private final String type;
        private final boolean primary;
        private final String locale;
        
        public AliasCandidate(String name, String type, boolean primary, String locale) {
            this.name = name;
            this.type = type;
            this.primary = primary;
            this.locale = locale;
        }
        
        public String getName() { return name; }
        public String getType() { return type; }
        public boolean isPrimary() { return primary; }
        public String getLocale() { return locale; }
    }
}

