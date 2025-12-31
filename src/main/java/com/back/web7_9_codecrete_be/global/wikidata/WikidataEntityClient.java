package com.back.web7_9_codecrete_be.global.wikidata;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class WikidataEntityClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String WIKIDATA_ENTITY_API = "https://www.wikidata.org/wiki/Special:EntityData/";

    /**
     * QID로 Entity 정보 조회
     */
    public Optional<JsonNode> getEntityInfo(String qid) {
        try {
            String url = WIKIDATA_ENTITY_API + qid + ".json";
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return Optional.empty();
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode entity = root.path("entities").path(qid);
            if (entity.isMissingNode() || entity.isNull()) return Optional.empty();

            return Optional.of(entity);
        } catch (Exception e) {
            log.warn("Wikidata entity 조회 실패: {}", qid, e);
            return Optional.empty();
        }
    }

    /**
     * Entity ID 타입 claim에서 QID 값 반환
     */
    public Optional<String> getEntityIdClaim(JsonNode entity, String propertyId) {
        JsonNode claims = entity.path("claims").path(propertyId);
        if (!claims.isArray() || claims.isEmpty()) return Optional.empty();

        JsonNode value = claims.get(0)
                .path("mainsnak")
                .path("datavalue")
                .path("value");

        JsonNode idNode = value.path("id");
        if (!idNode.isMissingNode() && !idNode.asText().isBlank()) {
            return Optional.of(idNode.asText());
        }
        return Optional.empty();
    }

    /**
     * claims[propertyId]의 모든 QID 값 반환
     */
    public List<String> getAllEntityIdClaims(JsonNode entity, String propertyId) {
        List<String> results = new ArrayList<>();
        JsonNode claims = entity.path("claims").path(propertyId);
        if (!claims.isArray() || claims.isEmpty()) return results;

        for (JsonNode claim : claims) {
            JsonNode value = claim
                    .path("mainsnak")
                    .path("datavalue")
                    .path("value");

            JsonNode idNode = value.path("id");
            if (!idNode.isMissingNode() && !idNode.asText().isBlank()) {
                results.add(idNode.asText());
            }
        }
        return results;
    }

    /**
     * String 타입 claim 값 가져오기
     */
    public Optional<String> getStringClaim(JsonNode entity, String propertyId) {
        try {
            JsonNode claims = entity.path("claims").path(propertyId);
            if (!claims.isArray() || claims.isEmpty()) {
                return Optional.empty();
            }

            JsonNode value = claims.get(0)
                    .path("mainsnak")
                    .path("datavalue")
                    .path("value");

            if (!value.isMissingNode() && !value.asText().isBlank()) {
                return Optional.of(value.asText());
            }

            return Optional.empty();
        } catch (Exception e) {
            log.debug("String claim 가져오기 실패: propertyId={}", propertyId, e);
            return Optional.empty();
        }
    }

    /**
     * Time 타입 claim 값 가져오기 (P569 = date of birth 등)
     */
    public Optional<String> getTimeClaim(JsonNode entity, String propertyId) {
        try {
            JsonNode claims = entity.path("claims").path(propertyId);
            if (!claims.isArray() || claims.isEmpty()) {
                return Optional.empty();
            }

            JsonNode value = claims.get(0)
                    .path("mainsnak")
                    .path("datavalue")
                    .path("value");

            if (value.isMissingNode()) {
                return Optional.empty();
            }

            JsonNode time = value.path("time");
            if (!time.isMissingNode() && !time.asText().isBlank()) {
                String timeStr = time.asText();
                if (timeStr.startsWith("+") || timeStr.startsWith("-")) {
                    timeStr = timeStr.substring(1);
                }
                if (timeStr.contains("T")) {
                    timeStr = timeStr.split("T")[0];
                }
                return Optional.of(timeStr);
            }

            return Optional.empty();
        } catch (Exception e) {
            log.debug("Time claim 가져오기 실패: propertyId={}", propertyId, e);
            return Optional.empty();
        }
    }

    /**
     * Entity ID 타입 claim에서 label 가져오기 (P735, P734 등)
     */
    public Optional<String> getEntityLabelClaim(JsonNode entity, String propertyId) {
        try {
            JsonNode claims = entity.path("claims").path(propertyId);
            if (!claims.isArray() || claims.isEmpty()) {
                return Optional.empty();
            }

            JsonNode value = claims.get(0)
                    .path("mainsnak")
                    .path("datavalue")
                    .path("value");

            JsonNode idNode = value.path("id");
            if (idNode.isMissingNode() || idNode.asText().isBlank()) {
                return Optional.empty();
            }

            String qid = idNode.asText();

            JsonNode labels = entity.path("labels");

            JsonNode koLabel = labels.path("ko");
            if (!koLabel.isMissingNode()) {
                String label = koLabel.path("value").asText();
                if (label != null && !label.isBlank()) {
                    return Optional.of(label);
                }
            }

            JsonNode enLabel = labels.path("en");
            if (!enLabel.isMissingNode()) {
                String label = enLabel.path("value").asText();
                if (label != null && !label.isBlank()) {
                    return Optional.of(label);
                }
            }

            Optional<JsonNode> nameEntityOpt = getEntityInfo(qid);
            if (nameEntityOpt.isPresent()) {
                JsonNode nameEntity = nameEntityOpt.get();
                JsonNode nameLabels = nameEntity.path("labels");

                JsonNode koLabel2 = nameLabels.path("ko");
                if (!koLabel2.isMissingNode()) {
                    String label = koLabel2.path("value").asText();
                    if (label != null && !label.isBlank()) {
                        return Optional.of(label);
                    }
                }

                JsonNode enLabel2 = nameLabels.path("en");
                if (!enLabel2.isMissingNode()) {
                    String label = enLabel2.path("value").asText();
                    if (label != null && !label.isBlank()) {
                        return Optional.of(label);
                    }
                }
            }

            return Optional.empty();
        } catch (Exception e) {
            log.debug("Entity label claim 가져오기 실패: propertyId={}", propertyId, e);
            return Optional.empty();
        }
    }

    /**
     * Entity ID 타입 claim에서 label 가져오기 (국적 등)
     */
    public Optional<String> getCountryLabelFromEntityId(JsonNode entity, String propertyId) {
        try {
            JsonNode claims = entity.path("claims").path(propertyId);
            if (!claims.isArray() || claims.isEmpty()) {
                return Optional.empty();
            }

            JsonNode value = claims.get(0)
                    .path("mainsnak")
                    .path("datavalue")
                    .path("value");

            JsonNode idNode = value.path("id");
            if (idNode.isMissingNode() || idNode.asText().isBlank()) {
                return Optional.empty();
            }

            String qid = idNode.asText();

            Optional<JsonNode> countryEntityOpt = getEntityInfo(qid);
            if (countryEntityOpt.isPresent()) {
                JsonNode countryEntity = countryEntityOpt.get();
                JsonNode countryLabels = countryEntity.path("labels");

                JsonNode koLabel = countryLabels.path("ko");
                if (!koLabel.isMissingNode()) {
                    String label = koLabel.path("value").asText();
                    if (label != null && !label.isBlank()) {
                        return Optional.of(label);
                    }
                }

                JsonNode enLabel = countryLabels.path("en");
                if (!enLabel.isMissingNode()) {
                    String label = enLabel.path("value").asText();
                    if (label != null && !label.isBlank()) {
                        return Optional.of(label);
                    }
                }
            }

            return Optional.empty();
        } catch (Exception e) {
            log.debug("Country label 가져오기 실패: propertyId={}", propertyId, e);
            return Optional.empty();
        }
    }
}

