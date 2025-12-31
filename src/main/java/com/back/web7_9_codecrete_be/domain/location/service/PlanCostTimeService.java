package com.back.web7_9_codecrete_be.domain.location.service;

import com.back.web7_9_codecrete_be.domain.location.dto.plan.PlanCostTimeRequest;
import com.back.web7_9_codecrete_be.domain.location.dto.plan.PlanCostTimeResponse;
import com.back.web7_9_codecrete_be.domain.location.dto.request.tmap.TmapSummaryRequest;
import com.back.web7_9_codecrete_be.domain.location.dto.response.kakao.KakaoMobilityResponse;
import com.back.web7_9_codecrete_be.domain.location.dto.response.tmap.TmapSummaryAllResponse;
import com.back.web7_9_codecrete_be.global.error.code.LocationErrorCode;
import com.back.web7_9_codecrete_be.global.error.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import static com.back.web7_9_codecrete_be.domain.plans.entity.Schedule.TransportType.PUBLIC_TRANSPORT;

@Service
@RequiredArgsConstructor
public class PlanCostTimeService {
    private final RestClient kakaoMobilityClient;
    private final RestClient tmapRestClient;

//일단 대중교통 이용(tmap), 자차 이용(kakao) but 경유지 없는경우만 생각
    public PlanCostTimeResponse getCostTime(PlanCostTimeRequest request){

        if(request.getTransportType() != PUBLIC_TRANSPORT){
            PlanCostTimeResponse response = kakaoMobilityClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1/directions")
                            .queryParam("origin", request.getStartX() + "," + request.getStartY())
                            .queryParam("destination", request.getEndX() + "," + request.getEndY())
                            .queryParam("priority", "TIME")
                            .queryParam("summary", "false")
                            .build()
                    )
                    .retrieve()
                    .body(PlanCostTimeResponse.class);
            return response;

        }else{
            PlanCostTimeResponse response = tmapRestClient.post()
                    .uri("/transit/routes/sub")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new TmapSummaryRequest(
                            request.getStartX(), request.getStartY(),
                            request.getEndX(), request.getEndY(),
                            1,
                            "json"
                    )).retrieve()
                    .body(PlanCostTimeResponse.class);
            return response;
        }
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



}
