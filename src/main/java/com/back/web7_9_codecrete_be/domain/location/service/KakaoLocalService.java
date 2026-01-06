package com.back.web7_9_codecrete_be.domain.location.service;

import com.back.web7_9_codecrete_be.domain.location.dto.response.kakao.KakaoCoordinateResponse;
import com.back.web7_9_codecrete_be.domain.location.dto.response.kakao.KakaoLocalResponse;
import com.back.web7_9_codecrete_be.domain.location.dto.response.kakao.KakaoMobilityResponse;
import com.back.web7_9_codecrete_be.domain.location.dto.request.kakao.KakaoRouteTransitRequest;
//import com.back.web7_9_codecrete_be.domain.location.dto.response.KakaoRouteTransitFeResponse;
import com.back.web7_9_codecrete_be.domain.location.dto.response.kakao.KakaoRouteTransitResponse;
import com.back.web7_9_codecrete_be.global.error.code.LocationErrorCode;
import com.back.web7_9_codecrete_be.global.error.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
@RequiredArgsConstructor
public class KakaoLocalService {

    private final RestClient kakaoRestClient;
    private final RestClient kakaoMobilityClient;

    // 해당 좌표의 1km 근방에 존재하는 음식점를 거리순으로 나타냄
    @Retryable(     //최초 1번, 재시도 2번 시도
            retryFor = {HttpServerErrorException.class, ResourceAccessException.class},     //외부 서버의 문제, 네트워크, 타임아웃 문제인 경우에 재시도
            backoff = @Backoff(delay = 200, multiplier = 2.0)   //0.2초, 0.4초, 0.8초 순으로 재시도
    )
    public List<KakaoLocalResponse.Document> searchNearbyRestaurants(double lat, double lng) {
        //좌표의 소수점 숫자가 다르면 매번 다른 캐싱을 해야하니, 통일시켜줌
        lat = Math.round(lat * 10000) / 10000.0;
        lng = Math.round(lng * 10000) / 10000.0;
        final double y = lat;
        final double x = lng;
        return kakaoRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v2/local/search/keyword.json")
                        .queryParam("query", "음식점")
                        .queryParam("category_group_code", "FD6")
                        .queryParam("x", x)
                        .queryParam("y", y)
                        .queryParam("radius", 1000)  // 반경 1km
                        .queryParam("sort", "distance")
                        .build()
                )
                .retrieve()
                .body(KakaoLocalResponse.class)
                .getDocuments();
    }

    // 해당 좌표의 1km 근방에 존재하는 카페를 거리순으로 나타냄
    @Retryable(     //최초 1번, 재시도 2번 시도
            retryFor = {HttpServerErrorException.class, ResourceAccessException.class},     //외부 서버의 문제, 네트워크, 타임아웃 문제인 경우에 재시도
            backoff = @Backoff(delay = 200, multiplier = 2.0)   //0.2초, 0.4초, 0.8초 순으로 재시도
    )
     public List<KakaoLocalResponse.Document> searchNearbyCafes(double lat, double lng) {

        //좌표의 소수점 숫자가 다르면 매번 다른 캐싱을 해야하니, 통일시켜줌
        lat = Math.round(lat * 10000) / 10000.0;
        lng = Math.round(lng * 10000) / 10000.0;
        final double y = lat;
        final double x = lng;
        return kakaoRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v2/local/search/keyword.json")
                        .queryParam("query", "카페")
                        .queryParam("category_group_code", "CE7")
                        .queryParam("x", x)
                        .queryParam("y", y)
                        .queryParam("radius", 1000)  // 반경 1km
                        .queryParam("sort", "distance")
                        .build()
                )
                .retrieve()
                .body(KakaoLocalResponse.class)
                .getDocuments();
    }

    //좌표를 주소로 변환해주는 api 연동
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

    //카카오 모빌리티에서 전체 응답값 가져오기
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

    //카카오 자동차에서 summary부분만 가져오기
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


    //Kakao mobility api에서 경유지가 있을때
    public KakaoRouteTransitResponse NaviSearchTransit(KakaoRouteTransitRequest transitRequest){


        //카카오가 원하는 요청값을 만들어주고 보내야함 (필수값들)
        KakaoRouteTransitRequest transit = new KakaoRouteTransitRequest();
        transit.setOrigin(transitRequest.getOrigin());
        transit.setDestination(transitRequest.getDestination());
        transit.setWaypoints(transitRequest.getWaypoints());

        transit.setPriority("TIME");
        transit.setSummary(false);
        transit.setCar_fuel("GASOLINE");
        transit.setCar_hipass(false);

        KakaoRouteTransitResponse response = kakaoMobilityClient.post()
                .uri("/v1/waypoints/directions")
                .body(transit)
                .retrieve()
                .body(KakaoRouteTransitResponse.class);     //KakaoRouteTransitResponse로 카카오 자동차 api에서 주는 응답값
        return response;
    }
}