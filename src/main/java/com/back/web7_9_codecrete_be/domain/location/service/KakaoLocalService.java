package com.back.web7_9_codecrete_be.domain.location.service;

import com.back.web7_9_codecrete_be.domain.location.dto.KakaoCoordinateResponse;
import com.back.web7_9_codecrete_be.domain.location.dto.KakaoLocalResponse;
import com.back.web7_9_codecrete_be.global.error.code.LocationErrorCode;
import com.back.web7_9_codecrete_be.global.error.exception.BusinessException;
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

    public String coordinateToAddressName(double lat, double lng) {

        KakaoCoordinateResponse response = kakaoWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v2/local/geo/coord2address.json")
                        .queryParam("x", lng)
                        .queryParam("y", lat)
                        .build()
                )
                .retrieve()
                .bodyToMono(KakaoCoordinateResponse.class)
                .block();

        if (response == null || response.getDocuments() == null || response.getDocuments().isEmpty()) {
            throw new BusinessException(LocationErrorCode.ADDRESS_NOT_FOUND);
        }

        KakaoCoordinateResponse.Document doc = response.getDocuments().get(0);

        String addressName = null;
        if (doc.getRoad_address() != null && doc.getRoad_address().getAddress_name() != null) {
            addressName = doc.getRoad_address().getAddress_name();
        } else if (doc.getAddress() != null) {
            addressName = doc.getAddress().getAddress_name();
        }

        if (addressName == null || addressName.isBlank()) {
            throw new BusinessException(LocationErrorCode.ADDRESS_NOT_FOUND);
        }

        return addressName;
    }

}