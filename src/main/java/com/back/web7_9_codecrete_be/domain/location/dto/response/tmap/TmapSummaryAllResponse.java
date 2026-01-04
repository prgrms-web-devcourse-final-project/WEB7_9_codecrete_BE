package com.back.web7_9_codecrete_be.domain.location.dto.response.tmap;

import lombok.Getter;

import java.util.List;
@Getter
public class TmapSummaryAllResponse {           // Tmap 대중교통 요약 api response dto

    private MetaData metaData;

    @Getter
    public static class MetaData {
        private Plan plan;
        private RequestParameters requestParameters;
    }

    @Getter
    public static class Plan {
        private List<Itinerary> itineraries;
    }

    @Getter
    public static class Itinerary {

        private int pathType;               // 경로 탐색 결과 종류
        private int totalTime;              // 총 소요시간 (sec)
        private int transferCount;          // 환승 횟수
        private int totalWalkDistance;      // 총 보행 거리 (m)
        private int totalDistance;          // 총 이동 거리 (m)
        private int totalWalkTime;          // 총 보행 시간 (sec)

        private Fare fare;                  // 요금 정보
    }

    @Getter
    public static class Fare {
        private Regular regular;
    }

    @Getter
    public static class Regular {
        private Currency currency;
        private int totalFare;              // 대중교통 요금
    }

    @Getter
    public static class Currency {
        private String symbol;              // ￦
        private String currency;            // 원
        private String currencyCode;        // KRW
    }

    @Getter
    public static class RequestParameters {

        private String reqDttm;              // 요청 시각

        private String startX;               // 출발지 경도
        private String startY;               // 출발지 위도
        private String endX;                 // 도착지 경도
        private String endY;                 // 도착지 위도
    }

}
