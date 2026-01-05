package com.back.web7_9_codecrete_be.domain.location.dto.fe;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "Tmap 도보 경로 요약 응답 DTO")
public class TmapWalkFeResponse {

    @Schema(
            description = "총 이동 거리 (미터 단위)",
            example = "3245"
    )
    private int totalDistance;

    @Schema(
            description = "총 소요 시간 (초 단위)",
            example = "2510"
    )
    private int totalTime;
}
