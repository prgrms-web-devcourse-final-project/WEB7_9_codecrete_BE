package com.back.web7_9_codecrete_be.domain.location.service;

import com.back.web7_9_codecrete_be.domain.location.dto.plan.PlanCostTimeRequest;
import com.back.web7_9_codecrete_be.domain.location.dto.plan.PlanCostTimeResponse;
import com.back.web7_9_codecrete_be.domain.location.dto.request.tmap.TmapSummaryRequest;
import com.back.web7_9_codecrete_be.domain.location.dto.response.kakao.KakaoMobilityResponse;
import com.back.web7_9_codecrete_be.domain.location.dto.response.tmap.TmapSummaryAllResponse;
import com.back.web7_9_codecrete_be.domain.location.dto.response.tmap.TmapSummaryResponse;
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
public PlanCostTimeResponse getCostTime(PlanCostTimeRequest request) {

    if (request.getTransportType() != PUBLIC_TRANSPORT) {           //대중교통인경우
        KakaoMobilityResponse kakao = kakaoMobilityClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1/directions")
                        .queryParam("origin", request.getStartX() + "," + request.getStartY())
                        .queryParam("destination", request.getEndX() + "," + request.getEndY())
                        .queryParam("priority", "TIME")
                        .queryParam("summary", "true")   // 비용/시간만 뽑기 때문에 요약한 부분이 필요
                        .build()
                )
                .retrieve()
                .body(KakaoMobilityResponse.class);

        return toPlanCostTimeResponseFromKakao(kakao);

    } else {            //자차인 경우
        TmapSummaryRequest tmapReq = new TmapSummaryRequest(
                request.getStartX(), request.getStartY(),
                request.getEndX(), request.getEndY(),
                1,
                "json"
        );

        TmapSummaryResponse tmap = tmapRestClient.post()
                .uri("/transit/routes/sub")
                .contentType(MediaType.APPLICATION_JSON)
                .body(tmapReq)
                .retrieve()
                .body(TmapSummaryResponse.class);

        return toPlanCostTimeResponseFromTmap(tmap);
    }
}

//밑의 함수들은 카카오 및 tmap에서 가져온 응답에서 시간, 비용만 필요하기 때문에 필터링을 하는 함수

    private PlanCostTimeResponse toPlanCostTimeResponseFromKakao(KakaoMobilityResponse kakao) {
        if (kakao == null || kakao.getRoutes() == null || kakao.getRoutes().isEmpty()                // 검증 로직을 따로 함수로 만들까 고민중.. 어떻게 생각하시나요
                || kakao.getRoutes().get(0).getSummary() == null) {
            throw new BusinessException(LocationErrorCode.ROUTE_NOT_FOUND);
        }

        int duration = kakao.getRoutes().get(0).getSummary().getDuration();

        PlanCostTimeResponse res = new PlanCostTimeResponse();
        res.setTime(duration);   // 초
        res.setCost(null);       // 자차로 가는 경우에는 추가 비용이 들지 않기 때문에 null
        return res;
    }

    private PlanCostTimeResponse toPlanCostTimeResponseFromTmap(TmapSummaryResponse tmap) {
        if (tmap == null || tmap.getMetaData() == null || tmap.getMetaData().getPlan() == null          // 검증 로직을 따로 함수로 만들까 고민중.. 어떻게 생각하시나요
                || tmap.getMetaData().getPlan().getItineraries() == null
                || tmap.getMetaData().getPlan().getItineraries().isEmpty()) {
            throw new BusinessException(LocationErrorCode.ROUTE_NOT_FOUND);
        }

        TmapSummaryResponse.Itinerary it = tmap.getMetaData().getPlan().getItineraries().get(0);

        Integer time = it.getTotalTime();
        Integer cost = null;
        if (it.getFare() != null && it.getFare().getRegular() != null) {
            cost = it.getFare().getRegular().getTotalFare();
        }

        PlanCostTimeResponse res = new PlanCostTimeResponse();
        res.setTime(time);
        res.setCost(cost);
        return res;
    }

}
