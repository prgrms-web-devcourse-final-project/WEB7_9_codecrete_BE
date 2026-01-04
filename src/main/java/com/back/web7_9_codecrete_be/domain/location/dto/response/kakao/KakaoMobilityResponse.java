package com.back.web7_9_codecrete_be.domain.location.dto.response.kakao;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "카카오 모빌리티 길찾기 API 응답 DTO")
public class KakaoMobilityResponse {

    @Schema(description = "경로 목록")
    private List<Route> routes;

    @Getter
    @Schema(description = "경로 정보")
    public static class Route {
        @Schema(description = "경로 요약 정보")
        private Summary summary;

        @Schema(description = "경로 구간 목록")
        private List<Section> sections;
    }

    @Getter
    @Schema(description = "경로 요약")
    public static class Summary {

        @Schema(description = "총 거리 (meter)", example = "1250")
        private int distance;

        @Schema(description = "총 소요 시간 (second)", example = "540")
        private int duration;
    }

    @Getter
    @Schema(description = "경로 구간")
    public static class Section {

        @Schema(description = "도로 정보 목록")
        private List<Road> roads;

        @Schema(description = "안내 정보 목록")
        private List<Guide> guides;
    }

    @Getter
    @Schema(description = "도로 정보")
    public static class Road {

        @Schema(description = "도로 좌표 목록 (x1, y1, x2, y2 ...)")
        private List<Double> vertexes;
    }

    @Getter
    @Schema(description = "길 안내 정보")
    public static class Guide {

        @Schema(description = "안내 지점 이름", example = "광화문 사거리")
        private String name;

        @Schema(description = "경도(longitude)", example = "126.9780")
        private double x;

        @Schema(description = "위도(latitude)", example = "37.5665")
        private double y;

        @Schema(description = "구간 거리 (meter)", example = "200")
        private int distance;

        @Schema(description = "구간 소요 시간 (second)", example = "90")
        private int duration;

        @Schema(description = "안내 타입", example = "1")
        private int type;

        @Schema(description = "안내 문구", example = "우회전하세요")
        private String guidance;

        @Schema(description = "도로 인덱스", example = "0")
        private int road_index;
    }
}
