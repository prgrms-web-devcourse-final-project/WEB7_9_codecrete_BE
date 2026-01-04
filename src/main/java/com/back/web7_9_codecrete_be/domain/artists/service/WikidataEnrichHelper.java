package com.back.web7_9_codecrete_be.domain.artists.service;

import com.back.web7_9_codecrete_be.global.wikidata.WikidataClient;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class WikidataEnrichHelper {

    private final WikidataClient wikidataClient;

    // Wikidata 엔티티에서 아티스트 타입 추출
    public String inferArtistType(JsonNode entity) {
        List<String> instanceOfList = wikidataClient.getAllEntityIdClaims(entity, "P31");
        boolean isGroup = instanceOfList.contains("http://www.wikidata.org/entity/Q215380");
        boolean isHuman = instanceOfList.contains("http://www.wikidata.org/entity/Q5");

        if (isGroup) {
            return "GROUP";
        } else if (isHuman) {
            return "SOLO";
        }

        return null;
    }

    // Wikidata 엔티티에서 소속 그룹 이름 추출
    public String resolveGroupName(JsonNode artistEntity) {
        List<String> memberOfQids = wikidataClient.getAllEntityIdClaims(artistEntity, "P463");
        if (memberOfQids.isEmpty()) {
            return null;
        }

        List<GroupCandidate> candidates = new ArrayList<>();
        
        for (String qid : memberOfQids) {
            Optional<JsonNode> entityOpt = wikidataClient.getEntityInfo(qid);
            if (entityOpt.isEmpty()) continue;

            JsonNode entity = entityOpt.get();
            List<String> instanceOfList = wikidataClient.getAllEntityIdClaims(entity, "P31");
            boolean isGroup = instanceOfList.contains("http://www.wikidata.org/entity/Q215380");
            if (!isGroup) continue;
            
            String koLabel = entity.path("labels").path("ko").path("value").asText(null);
            Optional<String> nameKoOpt = wikidataClient.getKoreanNameFromWikipedia(entity);
            String nameKo = nameKoOpt.orElse(null);
            String enLabel = entity.path("labels").path("en").path("value").asText(null);
            Optional<String> wikiTitleOpt = wikidataClient.getWikipediaKoreanTitle(entity);
            String wikiTitle = wikiTitleOpt.orElse(null);
            
            int score = 0;
            String selectedName = null;
            
            if (koLabel != null && !koLabel.isBlank()) {
                score += 100;
                selectedName = koLabel;
            } else if (nameKo != null && !nameKo.isBlank()) {
                score += 90;
                selectedName = nameKo;
            } else if (wikiTitle != null && !wikiTitle.isBlank()) {
                score += 80;
                selectedName = wikiTitle;
            } else if (enLabel != null && !enLabel.isBlank()) {
                score += 50;
                selectedName = enLabel;
            }
            
            List<String> mbids = wikidataClient.getAllEntityIdClaims(entity, "P434");
            if (!mbids.isEmpty()) {
                score += 20;
            }
            
            JsonNode sitelinks = entity.path("sitelinks");
            if (!sitelinks.isMissingNode() && sitelinks.size() > 0) {
                score += 10;
            }
            
            if (selectedName != null) {
                candidates.add(new GroupCandidate(qid, selectedName, score));
            }
        }

        if (candidates.isEmpty()) {
            return null;
        }

        candidates.sort((a, b) -> Integer.compare(b.score, a.score));
        return candidates.get(0).name;
    }

    // Wikidata 엔티티 검증 (QID 후보 검증)
    public int validateEntity(JsonNode entity, String artistName, String nameKo) {
        int score = 0;
        
        List<String> instanceOfList = wikidataClient.getAllEntityIdClaims(entity, "P31");
        boolean isGroup = instanceOfList.contains("http://www.wikidata.org/entity/Q215380");
        boolean isHuman = instanceOfList.contains("http://www.wikidata.org/entity/Q5");
        if (!isGroup && !isHuman) {
            return 0;
        }
        score += 50;
        
        String koLabel = entity.path("labels").path("ko").path("value").asText(null);
        String enLabel = entity.path("labels").path("en").path("value").asText(null);
        
        if (artistName != null && !artistName.isBlank()) {
            String artistNameLower = artistName.toLowerCase().trim();
            if (koLabel != null && koLabel.toLowerCase().contains(artistNameLower)) {
                score += 30;
            }
            if (enLabel != null && enLabel.toLowerCase().contains(artistNameLower)) {
                score += 30;
            }
        }
        
        if (nameKo != null && !nameKo.isBlank()) {
            String nameKoLower = nameKo.toLowerCase().trim();
            if (koLabel != null && koLabel.toLowerCase().contains(nameKoLower)) {
                score += 30;
            }
        }
        
        List<String> mbids = wikidataClient.getAllEntityIdClaims(entity, "P434");
        if (!mbids.isEmpty()) {
            score += 20;
        }
        
        JsonNode sitelinks = entity.path("sitelinks");
        if (!sitelinks.isMissingNode() && sitelinks.size() > 0) {
            score += 10;
        }
        
        return score;
    }

    private static class GroupCandidate {
        final String qid;
        final String name;
        final int score;
        
        GroupCandidate(String qid, String name, int score) {
            this.qid = qid;
            this.name = name;
            this.score = score;
        }
    }
}
