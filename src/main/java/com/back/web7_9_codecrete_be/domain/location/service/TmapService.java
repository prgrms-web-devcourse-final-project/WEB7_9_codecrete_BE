package com.back.web7_9_codecrete_be.domain.location.service;

import com.back.web7_9_codecrete_be.domain.location.dto.response.TmapRequest;
import com.back.web7_9_codecrete_be.domain.location.dto.request.TmapSummaryRequest;
import com.back.web7_9_codecrete_be.domain.location.dto.response.TmapSummaryAllResponse;
import com.back.web7_9_codecrete_be.domain.location.dto.response.TmapSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class TmapService {

    private final RestClient tmapRestClient;

    public TmapService(@Qualifier("tmapRestClient") RestClient tmapRestClient) {
        this.tmapRestClient = tmapRestClient;
    }
    public String getRoute(double startX, double startY, double endX, double endY) {

        TmapRequest req = new TmapRequest();
        req.setStartX(String.valueOf(startX));
        req.setStartY(String.valueOf(startY));
        req.setEndX(String.valueOf(endX));
        req.setEndY(String.valueOf(endY));
        req.setCount(5);
        req.setFormat("json");

        return tmapRestClient.post()
                .uri("/transit/routes")
                .contentType(MediaType.APPLICATION_JSON)
                .body(req)
                .retrieve()
                .body(String.class);
    }

    public TmapSummaryAllResponse getSummaryRoute(double startX, double startY, double endX, double endY){
        return tmapRestClient.post()
                .uri("/transit/routes/sub")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new TmapSummaryRequest(
                        startX, startY,
                        endX, endY,
                        1,
                        "json"
                ))
                .retrieve()
                .body(TmapSummaryAllResponse.class);
    }
}
