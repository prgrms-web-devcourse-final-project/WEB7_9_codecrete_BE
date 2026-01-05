package com.back.web7_9_codecrete_be.domain.artists.service.artistEnrichService;

import com.back.web7_9_codecrete_be.global.wikidata.WikidataClient;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class WikidataEnrichHelper {

    private final WikidataClient wikidataClient;

    // Wikidata 엔티티에서 아티스트 타입 추출
    public String inferArtistType(JsonNode entity) {
        List<String> instanceOfList = wikidataClient.getAllEntityIdClaims(entity, "P31");
        // QID 형식 확인: "Q215380" 또는 "http://www.wikidata.org/entity/Q215380" 모두 지원
        boolean isGroup = instanceOfList.contains("Q215380") || 
                         instanceOfList.contains("http://www.wikidata.org/entity/Q215380");
        boolean isHuman = instanceOfList.contains("Q5") || 
                         instanceOfList.contains("http://www.wikidata.org/entity/Q5");

        if (isGroup) {
            return "GROUP";
        } else if (isHuman) {
            return "SOLO";
        }

        return null;
    }

    // Wikidata 엔티티에서 소속 그룹 이름 추출
    public String resolveGroupName(JsonNode artistEntity) {
        // 아티스트 QID 추출 (로깅용)
        String artistQid = artistEntity.path("id").asText("unknown");
        String artistLabel = artistEntity.path("labels").path("ko").path("value").asText(
                artistEntity.path("labels").path("en").path("value").asText("unknown"));
        
        log.info("resolveGroupName 시작: artistQid={}, artistLabel={}", artistQid, artistLabel);
        
        List<String> memberOfQids = wikidataClient.getAllEntityIdClaims(artistEntity, "P463");
        log.info("P463 claims 개수: artistQid={}, count={}, qids={}", artistQid, memberOfQids.size(), memberOfQids);
        
        if (memberOfQids.isEmpty()) {
            log.warn("P463 claims가 비어있음: artistQid={}", artistQid);
            return null;
        }

        List<GroupCandidate> candidates = new ArrayList<>();
        
        for (String qid : memberOfQids) {
            log.info("그룹 후보 처리 시작: artistQid={}, groupQid={}", artistQid, qid);
            
            Optional<JsonNode> entityOpt = wikidataClient.getEntityInfo(qid);
            if (entityOpt.isEmpty()) {
                log.warn("그룹 엔티티 조회 실패: artistQid={}, groupQid={}", artistQid, qid);
                continue;
            }

            JsonNode entity = entityOpt.get();
            String groupLabel = entity.path("labels").path("ko").path("value").asText(
                    entity.path("labels").path("en").path("value").asText("unknown"));
            
            List<String> instanceOfList = wikidataClient.getAllEntityIdClaims(entity, "P31");
            log.info("그룹 P31 값: artistQid={}, groupQid={}, groupLabel={}, P31={}", 
                    artistQid, qid, groupLabel, instanceOfList);
            
            // QID에서 숫자만 추출 (SPARQL 쿼리용)
            String qidNumber = qid.replaceAll("[^0-9]", "");
            if (qidNumber.isBlank()) {
                log.warn("유효하지 않은 그룹 QID: artistQid={}, groupQid={}", artistQid, qid);
                continue;
            }
            
            // 1순위: SPARQL을 사용한 재귀적 그룹 판별 (가장 정확)
            // wdt:P31 / wdt:P279* 체인을 따라가며 Q215380에 도달 가능한지 확인
            boolean isGroup = wikidataClient.isMusicalGroupBySparql(qid);
            log.info("SPARQL 그룹 판별 결과: artistQid={}, groupQid={}, isGroup={}", 
                    artistQid, qid, isGroup);
            
            // 2순위: 빠른 폴백 - 직접 매칭 (SPARQL 실패 시 사용)
            if (!isGroup) {
                // QID 형식 확인: "Q215380" 또는 "http://www.wikidata.org/entity/Q215380" 모두 지원
                isGroup = instanceOfList.contains("Q215380") || 
                         instanceOfList.contains("http://www.wikidata.org/entity/Q215380");
                log.info("P31에서 Q215380(musical group) 직접 매칭: artistQid={}, groupQid={}, isGroup={}", 
                        artistQid, qid, isGroup);
                
                // P31이 Q215380이 아니어도, P279* (subclass of)로 Q215380의 하위 유형이면 그룹으로 인정
                if (!isGroup) {
                    // P279 (subclass of) claim 확인
                    List<String> subclassOfList = wikidataClient.getAllEntityIdClaims(entity, "P279");
                    log.info("그룹 P279 값: artistQid={}, groupQid={}, P279={}", artistQid, qid, subclassOfList);
                    
                    // 직접적인 subclass of가 Q215380인지 확인 (QID 형식 모두 지원)
                    isGroup = subclassOfList.contains("Q215380") || 
                             subclassOfList.contains("http://www.wikidata.org/entity/Q215380");
                    log.info("P279에서 Q215380 매칭: artistQid={}, groupQid={}, isGroup={}", 
                            artistQid, qid, isGroup);
                    
                    // P31이 하위 유형인 경우도 확인 (boy band, girl group, rock band 등)
                    // P31 값들 중 하나라도 musical group 관련이면 그룹으로 인정
                    if (!isGroup) {
                        for (String instanceOf : instanceOfList) {
                            // 일반적인 음악 그룹 관련 QID들 (주요 하위 유형)
                            // QID 형식 모두 지원: "Q215380" 또는 "http://www.wikidata.org/entity/Q215380"
                            if (instanceOf.contains("Q215380") || // musical group
                                instanceOf.contains("Q105543609") || // boy band
                                instanceOf.contains("Q105543608") || // girl group
                                instanceOf.contains("Q5741069") || // rock band
                                instanceOf.contains("Q2088357") || // pop group
                                instanceOf.contains("Q15975802")) { // K-pop group
                                isGroup = true;
                                log.info("P31 하위 유형에서 음악 그룹 매칭: artistQid={}, groupQid={}, matchedInstanceOf={}", 
                                        artistQid, qid, instanceOf);
                                break;
                            }
                        }
                    }
                }
            }
            
            log.info("최종 그룹 판정 결과: artistQid={}, groupQid={}, groupLabel={}, isGroup={}", 
                    artistQid, qid, groupLabel, isGroup);
            
            if (!isGroup) {
                log.warn("그룹이 아니어서 제외: artistQid={}, groupQid={}, groupLabel={}", 
                        artistQid, qid, groupLabel);
                continue;
            }
            
            String koLabel = entity.path("labels").path("ko").path("value").asText(null);
            Optional<String> nameKoOpt = wikidataClient.getKoreanNameFromWikipedia(entity);
            String nameKo = nameKoOpt.orElse(null);
            String enLabel = entity.path("labels").path("en").path("value").asText(null);
            Optional<String> wikiTitleOpt = wikidataClient.getWikipediaKoreanTitle(entity);
            String wikiTitle = wikiTitleOpt.orElse(null);
            
            log.info("그룹 이름 후보: artistQid={}, groupQid={}, koLabel={}, nameKo={}, enLabel={}, wikiTitle={}", 
                    artistQid, qid, koLabel, nameKo, enLabel, wikiTitle);
            
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
                log.info("그룹 후보 추가: artistQid={}, groupQid={}, selectedName={}, score={}", 
                        artistQid, qid, selectedName, score);
                candidates.add(new GroupCandidate(qid, selectedName, score));
            } else {
                log.warn("그룹 이름을 찾을 수 없어서 제외: artistQid={}, groupQid={}", artistQid, qid);
            }
        }

        log.info("그룹 후보 최종 개수: artistQid={}, candidateCount={}", artistQid, candidates.size());
        
        if (candidates.isEmpty()) {
            log.warn("그룹 후보가 없음: artistQid={}", artistQid);
            return null;
        }

        candidates.sort((a, b) -> Integer.compare(b.score, a.score));
        String finalGroupName = candidates.get(0).name;
        log.info("resolveGroupName 최종 결과: artistQid={}, selectedGroup={}, score={}", 
                artistQid, finalGroupName, candidates.get(0).score);
        return finalGroupName;
    }

    // Wikidata 엔티티 검증 (QID 후보 검증)
    public int validateEntity(JsonNode entity, String artistName, String nameKo) {
        int score = 0;
        
        List<String> instanceOfList = wikidataClient.getAllEntityIdClaims(entity, "P31");
        // QID 형식 확인: "Q215380" 또는 "http://www.wikidata.org/entity/Q215380" 모두 지원
        boolean isGroup = instanceOfList.contains("Q215380") || 
                         instanceOfList.contains("http://www.wikidata.org/entity/Q215380");
        boolean isHuman = instanceOfList.contains("Q5") || 
                         instanceOfList.contains("http://www.wikidata.org/entity/Q5");
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
