package com.back.web7_9_codecrete_be.domain.location.dto.response.tmap;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.util.List;

@Getter
@Schema(description = "Tmap 대중교통 요약 전체 응답 DTO")
public class TmapSummaryAllResponse {

    @Schema(description = "메타 데이터")
    private MetaData metaData;

    @Getter
    @Schema(description = "응답 메타 데이터")
    public static class MetaData {

        @Schema(description = "경로 계획 정보")
        private Plan plan;

        @Schema(description = "요청 파라미터 정보")
        private RequestParameters requestParameters;
    }

    @Getter
    @Schema(description = "경로 계획")
    public static class Plan {

        @Schema(description = "경로 목록")
        private List<Itinerary> itineraries;
    }

    @Getter
    @Schema(description = "경로 상세 정보")
    public static class Itinerary {

        @Schema(description = "경로 타입", example = "1")
        private int pathType;

        @Schema(description = "총 소요 시간 (초)", example = "3600")
        private int totalTime;

        @Schema(description = "환승 횟수", example = "1")
        private int transferCount;

        @Schema(description = "총 보행 거리 (m)", example = "850")
        private int totalWalkDistance;

        @Schema(description = "총 이동 거리 (m)", example = "12000")
        private int totalDistance;

        @Schema(description = "총 보행 시간 (초)", example = "600")
        private int totalWalkTime;

        @Schema(description = "요금 정보")
        private Fare fare;
    }

    @Getter
    @Schema(description = "요금 정보")
    public static class Fare {

        @Schema(description = "일반 요금")
        private Regular regular;
    }

    @Getter
    @Schema(description = "일반 요금 상세")
    public static class Regular {

        @Schema(description = "통화 정보")
        private Currency currency;

        @Schema(description = "총 대중교통 요금", example = "1350")
        private int totalFare;
    }

    @Getter
    @Schema(description = "통화 정보")
    public static class Currency {

        @Schema(description = "통화 기호", example = "￦")
        private String symbol;

        @Schema(description = "통화 단위", example = "원")
        private String currency;

        @Schema(description = "통화 코드", example = "KRW")
        private String currencyCode;
    }

    @Getter
    @Schema(description = "요청 파라미터 정보")
    public static class RequestParameters {

        @Schema(description = "요청 시각", example = "20251226103000")
        private String reqDttm;

        @Schema(description = "출발지 경도", example = "126.977969")
        private String startX;

        @Schema(description = "출발지 위도", example = "37.566535")
        private String startY;

        @Schema(description = "도착지 경도", example = "126.986037")
        private String endX;

        @Schema(description = "도착지 위도", example = "37.563617")
        private String endY;
    }
}
