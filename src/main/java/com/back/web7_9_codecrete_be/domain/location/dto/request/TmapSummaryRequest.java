package com.back.web7_9_codecrete_be.domain.location.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TmapSummaryRequest {

    @Schema(description = "출발지 경도 (longitude)", example = "126.9780")
    private double startX;

    @Schema(description = "출발지 위도 (latitude)", example = "37.5665")
    private double startY;

    @Schema(description = "도착지 경도 (longitude)", example = "127.0276")
    private double endX;

    @Schema(description = "도착지 위도 (latitude)", example = "37.4979")
    private double endY;

    @Schema(description = "최대 응답 결과 개수", example = "5")
    private int count;

    @Schema(
            description = "응답 포맷 (json / xml)",
            example = "json",
            allowableValues = {"json", "xml"}
    )
    private String format;
}
