package com.back.web7_9_codecrete_be.domain.location.dto.response.tmap;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Schema(description = "Tmap 보행자 경로 원본 응답 DTO, 여기서 시간, 거리만 필요")
public class TmapWalkResponse {

    @Schema(description = "경로 정보 Feature 목록")
    private List<Feature> features;

    @Getter
    @NoArgsConstructor
    @Schema(description = "경로 Feature")
    public static class Feature {

        @Schema(description = "경로 Geometry 정보")
        private Geometry geometry;

        @Schema(description = "경로 요약 Properties")
        private Properties properties;
    }

    @Getter
    @NoArgsConstructor
    @Schema(description = "Geometry 정보 (Point 또는 LineString)")
    public static class Geometry {

        @Schema(
                description = "Geometry 타입",
                example = "LineString"
        )
        private String type;

        @Schema(
                description = """
                        좌표 정보.
                        - LineString: [[x1,y1], [x2,y2], ...]
                        - Point: [x, y]
                        """,
                example = "[[126.97,37.56],[126.98,37.57]]"
        )
        private Object coordinates;
    }

    @Getter
    @NoArgsConstructor
    @Schema(description = "경로 요약 정보")
    public static class Properties {

        @Schema(
                description = "총 이동 거리 (미터)",
                example = "3245"
        )
        private Integer totalDistance;

        @Schema(
                description = "총 소요 시간 (초)",
                example = "2510"
        )
        private Integer totalTime;

        @Schema(
                description = "포인트 타입 (출발/도착 등)",
                example = "S"
        )
        private String pointType;
    }
}
