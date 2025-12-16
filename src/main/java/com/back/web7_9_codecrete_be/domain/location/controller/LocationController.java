package com.back.web7_9_codecrete_be.domain.location.controller;

import com.back.web7_9_codecrete_be.domain.location.dto.request.LocationRequestDto;
import com.back.web7_9_codecrete_be.domain.location.dto.response.LocationResponseDto;
import com.back.web7_9_codecrete_be.domain.location.service.LocationService;
import com.back.web7_9_codecrete_be.domain.users.entity.User;
import com.back.web7_9_codecrete_be.global.rq.Rq;
import com.back.web7_9_codecrete_be.global.rsData.RsData;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/location/my")
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;
    private final Rq rq;

    @GetMapping()
    public RsData<LocationResponseDto> getMyLocation(
    ){
        User user = rq.getUser();
        return RsData.success("내 위치정보를 조회합니다", locationService.getLocation(user));
    }


    @PostMapping()
    public RsData<LocationResponseDto> saveMyLocation(
            @RequestBody LocationRequestDto locationRequestDto
            ){
        User user = rq.getUser();
        return RsData.success("내 위치정보를 저장했습니다.", locationService.saveLocation(locationRequestDto, user));
    }


    @PatchMapping
    public RsData<LocationResponseDto> modifyMyLocation(
            @RequestBody LocationRequestDto locationRequestDto
            ){
        User user = rq.getUser();
        return RsData.success("내 위치정보를 수정했습니다.", locationService.modifyLocation(locationRequestDto, user));
    }

    @DeleteMapping
    public RsData<Void> deleteMyLocation(
    ){
        User user = rq.getUser();
        locationService.removeLocation(user);
        return RsData.success("내 위치정보를 삭제했습니다.", null);
    }
}
