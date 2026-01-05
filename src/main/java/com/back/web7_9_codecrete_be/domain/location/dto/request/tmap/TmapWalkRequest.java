package com.back.web7_9_codecrete_be.domain.location.dto.request.tmap;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "Tmap 보행자 경로 요청 DTO")
public class TmapWalkRequest {

    @Schema(
            description = "출발지 경도 (longitude)",
            example = "126.9780",
            required = true
    )
    @JsonProperty("startX")
    private double startX;

    @Schema(
            description = "출발지 위도 (latitude)",
            example = "37.5665",
            required = true
    )
    @JsonProperty("startY")
    private double startY;

    @Schema(
            description = "도착지 경도 (longitude)",
            example = "127.0276",
            required = true
    )
    @JsonProperty("endX")
    private double endX;

    @Schema(
            description = "도착지 위도 (latitude)",
            example = "37.4979",
            required = true
    )
    @JsonProperty("endY")
    private double endY;

    @Schema(
            description = "출발지 이름 (좌표만 있을 경우 임의 문자열 가능)",
            example = "출발지"
    )
    @JsonProperty("startName")
    private String startName;

    @Schema(
            description = "도착지 이름 (좌표만 있을 경우 임의 문자열 가능)",
            example = "도착지"
    )
    @JsonProperty("endName")
    private String endName;

    @Schema(
            description = "요청 좌표계 타입",
            example = "WGS84GEO",
            allowableValues = {"WGS84GEO"}
    )
    @JsonProperty("reqCoordType")
    private String reqCoordType;

    @Schema(
            description = "응답 좌표계 타입",
            example = "WGS84GEO",
            allowableValues = {"WGS84GEO"}
    )
    @JsonProperty("resCoordType")
    private String resCoordType;
}
