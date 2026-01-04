package com.back.web7_9_codecrete_be.domain.location.dto.request.kakao;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "카카오 모빌리티 경로 탐색 요청 DTO (경유지 포함)")
public class KakaoRouteTransitRequest {        //Kakao mobility api에서 경유지 추가 response

    @Schema(description = "출발지", requiredMode = Schema.RequiredMode.REQUIRED)
    private Origin origin;

    @Schema(description = "도착지", requiredMode = Schema.RequiredMode.REQUIRED)
    private Destination destination;

    @Schema(description = "경유지 목록")
    private List<Waypoint> waypoints;


    @Schema(description = "경로 우선순위", example = "RECOMMEND")
    private String priority;

    @Schema(description = "차량 연료 타입", example = "GASOLINE")
    private String car_fuel;
    @Schema(description = "하이패스 여부", example = "true")

    private boolean car_hipass;
    @Schema(description = "대안 경로 포함 여부", example = "false")
    private boolean alternatives;
    @Schema(description = "도로 상세 정보 포함 여부", example = "true")
    private boolean road_details;
    @Schema(description = "summary만 응답 받을지 여부", example = "false")
    private boolean summary;

    @Data
    @Schema(description = "출발지 정보")
    public static class Origin {
        @Schema(description = "경도(longitude)", example = "126.977969")
        private double x;
        @Schema(description = "위도(latitude)", example = "37.566535")
        private double y;
        @Schema(description = "출발 방향 각도(0~359)", example = "0")
        private int angle;
    }

    @Data
    @Schema(description = "도착지 정보")
    public static class Destination {
        @Schema(description = "경도(longitude)", example = "126.986037")
        private double x;
        @Schema(description = "위도(latitude)", example = "37.563617")
        private double y;
    }

    @Data
    @Schema(description = "경유지 정보")
    public static class Waypoint {
        @Schema(description = "경유지 이름", example = "광화문역")
        private String name;
        @Schema(description = "경도(longitude)", example = "126.9770")
        private double x;
        @Schema(description = "위도(latitude)", example = "37.5710")
        private double y;
    }

}
