package com.back.web7_9_codecrete_be.domain.location.controller;

import com.back.web7_9_codecrete_be.domain.location.dto.request.LocationRequestDto;
import com.back.web7_9_codecrete_be.domain.location.dto.response.LocationResponseDto;
import com.back.web7_9_codecrete_be.domain.location.service.LocationService;
import com.back.web7_9_codecrete_be.domain.users.entity.User;
import com.back.web7_9_codecrete_be.global.rq.Rq;
import com.back.web7_9_codecrete_be.global.rsData.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/location/my")
@RequiredArgsConstructor
@Tag(name = "Location", description = "위치(Location) 관련 API")
public class LocationController {

    private final LocationService locationService;
    private final Rq rq;

    @GetMapping()
    @Operation(summary = "사용자 위치 조회", description = "사용자의 위치 정보를 조회합니다.")
    public RsData<LocationResponseDto> getMyLocation(
    ){
        User user = rq.getUser();
        return RsData.success("내 위치정보를 조회합니다", locationService.getLocation(user));
    }


    @PostMapping()
    @Operation(summary = "사용자 위치 저장", description = "사용자의 위치 정보를 저장합니다.")
    public RsData<LocationResponseDto> saveMyLocation(
            @RequestBody LocationRequestDto locationRequestDto
            ){
        User user = rq.getUser();
        return RsData.success("내 위치정보를 저장했습니다.", locationService.saveLocation(locationRequestDto, user));
    }


    @PatchMapping
    @Operation(summary = "사용자 위치 수정", description = "사용자의 위치 정보를 수정합니다.")
    public RsData<LocationResponseDto> modifyMyLocation(
            @RequestBody LocationRequestDto locationRequestDto
            ){
        User user = rq.getUser();
        return RsData.success("내 위치정보를 수정했습니다.", locationService.modifyLocation(locationRequestDto, user));
    }

    @DeleteMapping
    @Operation(summary = "사용자 위치 삭제", description = "사용자의 위치 정보를 삭제합니다.")
    public RsData<Void> deleteMyLocation(
    ){
        User user = rq.getUser();
        locationService.removeLocation(user);
        return RsData.success("내 위치정보를 삭제했습니다.", null);
    }
}
