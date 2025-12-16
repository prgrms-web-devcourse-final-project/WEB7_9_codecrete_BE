package com.back.web7_9_codecrete_be.domain.location.dto.response;

import com.back.web7_9_codecrete_be.domain.location.entity.Location;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;


@Getter
@AllArgsConstructor
@Schema(description = "위치 응답 DTO")
public class LocationResponseDto {

    @Schema(description = "위도", example = "37.566535")
    private double lat;
    @Schema(description = "경도", example = "126.9779692")
    private double lon;
    @Schema(description = "주소", example = "서울특별시 중구 세종대로 110")
    private String address;


    public static LocationResponseDto from(Location location) {
        return new LocationResponseDto(
                location.getLat(),
                location.getLon(),
                location.getAddress()
        );
    }
}
