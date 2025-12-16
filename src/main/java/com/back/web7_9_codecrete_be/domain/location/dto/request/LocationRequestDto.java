package com.back.web7_9_codecrete_be.domain.location.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "위치 요청 DTO")
public class LocationRequestDto {

    @Schema(description = "위도", example = "37.566535")
    private double lat;

    @Schema(description = "경도", example = "126.9779692")
    private double lon;

}
