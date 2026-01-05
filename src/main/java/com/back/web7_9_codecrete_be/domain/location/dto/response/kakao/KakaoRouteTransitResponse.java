package com.back.web7_9_codecrete_be.domain.location.dto.response.kakao;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(description = "카카오 대중교통 경로 탐색 API 응답 DTO")
public class KakaoRouteTransitResponse {

    @Schema(description = "트랜잭션 ID")
    private String transId;

    @Schema(description = "경로 목록")
    private List<Route> routes;

    @Data
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @Schema(description = "경로 정보")
    public static class Route {

        @Schema(description = "결과 코드", example = "0")
        private int resultCode;

        @Schema(description = "결과 메시지", example = "길찾기 성공")
        private String resultMsg;

        @Schema(description = "경로 요약")
        private Summary summary;

        @Schema(description = "경로 구간 목록")
        private List<Section> sections;
    }

    @Data
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @Schema(description = "경로 요약 정보")
    public static class Summary {

        @Schema(description = "출발지 정보")
        private Origin origin;

        @Schema(description = "도착지 정보")
        private Destination destination;

        @Schema(description = "경유지 목록")
        private List<Waypoint> waypoints;

        @Schema(description = "경로 우선순위", example = "RECOMMEND")
        private String priority;

        @Schema(description = "경로 경계 좌표 (optional)")
        private Bound bound;

        @Schema(description = "요금 정보")
        private Fare fare;

        @Schema(description = "총 거리 (meter)", example = "8500")
        private int distance;

        @Schema(description = "총 소요 시간 (second)", example = "3200")
        private int duration;
    }

    @Data
    @Schema(description = "출발지")
    public static class Origin {

        @Schema(description = "출발지 명칭", example = "서울역")
        private String name;

        @Schema(description = "경도", example = "126.9723")
        private double x;

        @Schema(description = "위도", example = "37.5559")
        private double y;
    }

    @Data
    @Schema(description = "도착지")
    public static class Destination {

        @Schema(description = "도착지 명칭", example = "광화문")
        private String name;

        @Schema(description = "경도", example = "126.9780")
        private double x;

        @Schema(description = "위도", example = "37.5665")
        private double y;
    }

    @Data
    @Schema(description = "경유지")
    public static class Waypoint {

        @Schema(description = "경유지 명칭")
        private String name;

        @Schema(description = "경도")
        private double x;

        @Schema(description = "위도")
        private double y;
    }

    @Data
    @Schema(description = "경로 영역 경계")
    public static class Bound {

        @Schema(description = "최소 경도")
        private double minX;

        @Schema(description = "최소 위도")
        private double minY;

        @Schema(description = "최대 경도")
        private double maxX;

        @Schema(description = "최대 위도")
        private double maxY;
    }

    @Data
    @Schema(description = "요금 정보")
    public static class Fare {

        @Schema(description = "택시 요금", example = "12000")
        private int taxi;

        @Schema(description = "통행료", example = "0")
        private int toll;
    }

    @Data
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @Schema(description = "경로 구간")
    public static class Section {

        @Schema(description = "구간 거리")
        private int distance;

        @Schema(description = "구간 소요 시간")
        private int duration;

        @Schema(description = "구간 경계 좌표")
        private Bound bound;

        @Schema(description = "도로 목록")
        private List<Road> roads;

        @Schema(description = "안내 목록")
        private List<Guide> guides;
    }

    @Data
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @Schema(description = "도로 정보")
    public static class Road {

        @Schema(description = "도로명")
        private String name;

        @Schema(description = "도로 거리")
        private int distance;

        @Schema(description = "도로 소요 시간")
        private int duration;

        @Schema(description = "교통 속도")
        private double trafficSpeed;

        @Schema(description = "교통 상태")
        private int trafficState;

        @Schema(description = "좌표 목록")
        private List<Double> vertexes;
    }

    @Data
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @Schema(description = "안내 정보")
    public static class Guide {

        @Schema(description = "안내 지점 이름")
        private String name;

        @Schema(description = "경도")
        private double x;

        @Schema(description = "위도")
        private double y;

        @Schema(description = "구간 거리")
        private int distance;

        @Schema(description = "구간 소요 시간")
        private int duration;

        @Schema(description = "안내 타입")
        private int type;

        @Schema(description = "안내 문구")
        private String guidance;

        @Schema(description = "도로 인덱스")
        private int roadIndex;
    }
}
