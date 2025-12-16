package com.back.web7_9_codecrete_be.domain.location.dto.response;

import com.back.web7_9_codecrete_be.domain.location.entity.Location;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
public class LocationResponseDto {

    private double lat;

    private double lon;

    private String address;


    public static LocationResponseDto from(Location location) {
        return new LocationResponseDto(
                location.getLat(),
                location.getLon(),
                location.getAddress()
        );
    }
}
