package com.back.web7_9_codecrete_be.domain.location.service;

import com.back.web7_9_codecrete_be.domain.location.dto.response.TmapResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@RequiredArgsConstructor
public class TmapService {

    private final RestClient TmapRestClient;

    public String getRoute(double startX, double startY, double endX, double endY) {

        TmapResponse req = new TmapResponse();
        req.setStartX(String.valueOf(startX));
        req.setStartY(String.valueOf(startY));
        req.setEndX(String.valueOf(endX));
        req.setEndY(String.valueOf(endY));
        req.setCount(5);
        req.setFormat("json");

        return TmapRestClient.post()
                .uri("/transit/routes")
                .contentType(MediaType.APPLICATION_JSON)
                .body(req)
                .retrieve()
                .body(String.class);
    }
}
