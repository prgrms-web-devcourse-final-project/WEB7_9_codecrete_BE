package com.back.web7_9_codecrete_be.domain.location.service;

import com.back.web7_9_codecrete_be.domain.location.dto.KakaoLocalResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Service
@RequiredArgsConstructor
public class KakaoLocalService {

    private final WebClient kakaoWebClient;

    public List<KakaoLocalResponse.Document> searchNearbyRestaurants(double lat, double lng) {

        return kakaoWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v2/local/search/keyword.json")
                        .queryParam("query", "음식점")
                        .queryParam("y", lat)
                        .queryParam("x", lng)
                        .queryParam("radius", 1000)  // 반경 1km
                        .build()
                )
                .retrieve()
                .bodyToMono(KakaoLocalResponse.class)
                .block() // 동기 호출 (필요하면 비동기로 변경 가능)
                .getDocuments();
    }
}