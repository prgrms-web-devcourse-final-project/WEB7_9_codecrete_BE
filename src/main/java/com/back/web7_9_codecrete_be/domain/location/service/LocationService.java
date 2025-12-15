package com.back.web7_9_codecrete_be.domain.location.service;

import com.back.web7_9_codecrete_be.domain.location.dto.LocationRequestDto;
import com.back.web7_9_codecrete_be.domain.location.dto.LocationResponseDto;
import com.back.web7_9_codecrete_be.domain.location.entity.Location;
import com.back.web7_9_codecrete_be.domain.location.repository.LocationRepository;
import com.back.web7_9_codecrete_be.domain.users.entity.User;
import com.back.web7_9_codecrete_be.domain.users.repository.UserRepository;
import com.back.web7_9_codecrete_be.global.error.code.AuthErrorCode;
import com.back.web7_9_codecrete_be.global.error.code.ErrorCode;
import com.back.web7_9_codecrete_be.global.error.code.LocationErrorCode;
import com.back.web7_9_codecrete_be.global.error.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LocationService {

    private final LocationRepository locationRepository;
    private final UserRepository userRepository;

    public LocationResponseDto getLocation(LocationRequestDto locationRequestDto, Long memberId){

    }

    public LocationResponseDto saveLocation(LocationRequestDto locationRequestDto, Long memberId){
        User user = getLocation
    }

    public LocationResponseDto modifyLocation(LocationRequestDto locationRequestDto, Long memberId){

    }

    public void removeLoctaion(LocationRequestDto locationRequestDto, Long userId, Long locationId){
        Location location = getLocationEntity(locationId);
        locationRepository.delete(location);
    }


    public Location getLocationEntity(Long locationId){
        return locationRepository.findById(locationId).orElseThrow(
                () -> new BusinessException(LocationErrorCode.LOCATION_NOT_FOUND)
        );
    }

    public User getLocationById(Long userId){
        return userRepository.findById(userId).orElseThrow(
                () -> new BusinessException(AuthErrorCode.UNAUTHORIZED_USER)
        );
    }



}
