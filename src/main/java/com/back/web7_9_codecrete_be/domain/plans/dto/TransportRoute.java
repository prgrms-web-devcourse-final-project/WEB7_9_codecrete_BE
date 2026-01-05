package com.back.web7_9_codecrete_be.domain.plans.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 교통 경로 정보 DTO
 * 프론트엔드에서 제공하는 교통 경로 상세 정보를 담는 클래스
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "교통 경로 정보")
public class TransportRoute {
    
    @Schema(description = "총 소요 시간 (초)", example = "3600")
    private Integer totalTime;
    
    @Schema(description = "총 거리 (m)", example = "5000")
    private Integer totalDistance;
    
    @Schema(description = "총 도보 시간 (초) - 선택적", example = "600")
    private Integer totalWalkTime;
    
    @Schema(description = "총 도보 거리 (m) - 선택적", example = "500")
    private Integer totalWalkDistance;
    
    @Schema(description = "환승 횟수 - 선택적", example = "2")
    private Integer transferCount;
    
    @Schema(description = "도보, 대중교통 구간 배열")
    private List<Leg> leg;
    
    @Schema(description = "요금 정보")
    private Fare fare;
    
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "이동 구간 정보")
    public static class Leg {
        @Schema(description = "이동 수단", example = "SUBWAY", allowableValues = {"WALK", "BUS", "SUBWAY", "EXPRESSBUS", "TRAIN", "AIRPLANE", "FERRY"})
        private String mode;
        
        @Schema(description = "구간 시작점")
        private Location start;
        
        @Schema(description = "구간 도착점")
        private Location end;
        
        @Schema(description = "구간 소요 시간 (초)", example = "1800")
        private Integer sectionTime;
        
        @Schema(description = "노선명 (예: \"간선:472\", \"지하철 2호선\") - 선택적", example = "지하철 2호선")
        private String route;
        
        @Schema(description = "노선 색상 (Hex 코드) - 선택적", example = "#00A84D")
        private String routeColor;
        
        @Schema(description = "노선 타입 - 선택적", example = "1")
        private Integer type;
        
        @Schema(description = "경유 정류장/역 개수 - 선택적", example = "5")
        private Integer passStopCount;
        
        @Schema(description = "경로 선형 (지도 그리기용) - 선택적")
        private PassShape passShape;
    }
    
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "위치 정보 (위도/경도)")
    public static class Location {
        @Schema(description = "위치 이름", example = "강남역")
        private String name;
        
        @Schema(description = "경도", example = "127.0276")
        private Double lon;
        
        @Schema(description = "위도", example = "37.4979")
        private Double lat;
    }
    
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "경로 선형 정보")
    public static class PassShape {
        @Schema(description = "경로 선형 (지도 그리기용)", example = "LINESTRING(127.0276 37.4979, 127.0286 37.4989)")
        private String linestring;
    }
    
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "요금 정보")
    public static class Fare {
        @Schema(description = "총 요금 (원) - 도보는 0원, 대중교통은 합산금액, 자동차는 택시 + 톨게이트비", example = "1500")
        private Integer totalFare;
        
        @Schema(description = "택시 요금 (원) - 선택적", example = "10000")
        private Integer taxi;
        
        @Schema(description = "톨게이트 요금 (원) - 선택적", example = "2500")
        private Integer toll;
    }
}

