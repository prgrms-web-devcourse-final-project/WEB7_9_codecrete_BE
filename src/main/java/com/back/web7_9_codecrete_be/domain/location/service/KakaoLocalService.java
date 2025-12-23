package com.back.web7_9_codecrete_be.domain.location.service;

import com.back.web7_9_codecrete_be.domain.location.dto.KakaoCoordinateResponse;
import com.back.web7_9_codecrete_be.domain.location.dto.response.KakaoLocalResponse;
import com.back.web7_9_codecrete_be.domain.location.dto.response.KakaoMobilityResponse;
import com.back.web7_9_codecrete_be.domain.location.dto.response.KakaoRouteTransitResponse;
import com.back.web7_9_codecrete_be.global.error.code.LocationErrorCode;
import com.back.web7_9_codecrete_be.global.error.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Service
@RequiredArgsConstructor
public class KakaoLocalService {

    private final RestClient kakaoRestClient;
    private final RestClient kakaoMobilityClient;

    public List<KakaoLocalResponse.Document> searchNearbyRestaurants(double lat, double lng) {

        return kakaoRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v2/local/search/keyword.json")
                        .queryParam("query", "음식점")
                        .queryParam("category_group_code", "FD6")
                        .queryParam("y", lat)
                        .queryParam("x", lng)
                        .queryParam("radius", 1000)  // 반경 1km
                        .queryParam("sort", "distance")
                        .build()
                )
                .retrieve()
                .body(KakaoLocalResponse.class)
                .getDocuments();
    }
    public List<KakaoLocalResponse.Document> searchNearbyCafes(double lat, double lng) {

        return kakaoRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v2/local/search/keyword.json")
                        .queryParam("query", "카페")
                        .queryParam("category_group_code", "CE7")
                        .queryParam("y", lat)
                        .queryParam("x", lng)
                        .queryParam("radius", 1000)  // 반경 1km
                        .queryParam("sort", "distance")
                        .build()
                )
                .retrieve()
                .body(KakaoLocalResponse.class)
                .getDocuments();
    }

    public String coordinateToAddressName(double lat, double lng) {

        KakaoCoordinateResponse response = kakaoRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v2/local/geo/coord2address.json")
                        .queryParam("x", lng)
                        .queryParam("y", lat)
                        .build()
                )
                .retrieve()
                .body(KakaoCoordinateResponse.class);

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

    public KakaoMobilityResponse NaviSearch(double startX, double startY, double endX, double endY) {

        KakaoMobilityResponse response = kakaoMobilityClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1/directions")
                        .queryParam("origin", startX + "," + startY)
                        .queryParam("destination", endX + "," + endY)
                        .queryParam("priority", "TIME")
                        .queryParam("summary", "false")
                        .build()
                )
                .retrieve()
                .body(KakaoMobilityResponse.class);


        if (response == null || response.getRoutes().isEmpty()) {
            throw new BusinessException(LocationErrorCode.ROUTE_NOT_FOUND);
        }

        return response;
    }

    public KakaoMobilityResponse NaviSearchSummary(double startX, double startY, double endX, double endY) {

        KakaoMobilityResponse response = kakaoMobilityClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1/directions")
                        .queryParam("origin", startX + "," + startY)
                        .queryParam("destination", endX + "," + endY)
                        .queryParam("priority", "TIME")
                        .queryParam("summary", "true")
                        .build()
                )
                .retrieve()
                .body(KakaoMobilityResponse.class);


        if (response == null || response.getRoutes().isEmpty()) {
            throw new BusinessException(LocationErrorCode.ROUTE_NOT_FOUND);
        }

        return response;
    }

    public KakaoRouteTransitResponse NaviSearchTransit(double startX, double startY
    , double endX, double endY, double wayX, double wayY){
        KakaoRouteTransitResponse response = kakaoMobilityClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1/waypoints/directions")
                        .queryParam("origin", startX + "," + startY)
                        .queryParam("destination", endX + "," + endY)
                        .queryParam("waypoints", wayX + "," + wayY)
                        .queryParam("priority", "TIME")
                        .queryParam("summary", "false")
                        .build()
                )
                .retrieve()
                .body(KakaoRouteTransitResponse.class);
        return response;
    }
}