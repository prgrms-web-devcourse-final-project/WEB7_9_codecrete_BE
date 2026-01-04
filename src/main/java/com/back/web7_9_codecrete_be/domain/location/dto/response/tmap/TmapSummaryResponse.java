package com.back.web7_9_codecrete_be.domain.location.dto.response.tmap;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.util.List;

@Getter
@Schema(description = "Tmap 대중교통 요약 응답 DTO (간략)")
public class TmapSummaryResponse {

    @Schema(description = "메타 데이터")
    private MetaData metaData;

    @Getter
    @Schema(description = "응답 메타 데이터")
    public static class MetaData {

        @Schema(description = "경로 계획 정보")
        private Plan plan;
    }

    @Getter
    @Schema(description = "경로 계획")
    public static class Plan {

        @Schema(description = "경로 목록")
        private List<Itinerary> itineraries;
    }

    @Getter
    @Schema(description = "경로 요약 정보")
    public static class Itinerary {

        @Schema(description = "총 소요 시간 (초)", example = "3400")
        private int totalTime;

        @Schema(description = "총 이동 거리 (m)", example = "11500")
        private int totalDistance;

        @Schema(description = "환승 횟수", example = "2")
        private int transferCount;

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

        @Schema(description = "총 대중교통 요금", example = "1400")
        private int totalFare;
    }
}
