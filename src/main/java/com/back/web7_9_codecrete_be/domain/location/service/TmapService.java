package com.back.web7_9_codecrete_be.domain.location.service;

import com.back.web7_9_codecrete_be.domain.location.dto.KakaoLocalResponse;
import com.back.web7_9_codecrete_be.domain.location.dto.TmapResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@RequiredArgsConstructor
public class TmapService {

    private final WebClient TmapClient;

    public String getRoute(double startX, double startY, double endX, double endY) {

        TmapResponse request = new TmapResponse();
        request.setStartX(String.valueOf(startX));
        request.setStartY(String.valueOf(startY));
        request.setEndX(String.valueOf(endX));
        request.setEndY(String.valueOf(endY));
        request.setFormat("json");
        request.setCount(5);

        return TmapClient.post()
                .uri("/transit/routes")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }
}
