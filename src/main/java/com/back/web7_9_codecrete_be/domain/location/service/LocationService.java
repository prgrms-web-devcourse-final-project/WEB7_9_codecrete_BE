package com.back.web7_9_codecrete_be.domain.location.service;

import com.back.web7_9_codecrete_be.domain.location.dto.KakaoLocalResponse;
import com.back.web7_9_codecrete_be.domain.location.dto.request.LocationRequestDto;
import com.back.web7_9_codecrete_be.domain.location.dto.response.LocationResponseDto;
import com.back.web7_9_codecrete_be.domain.location.entity.Location;
import com.back.web7_9_codecrete_be.domain.location.repository.LocationRepository;
import com.back.web7_9_codecrete_be.domain.users.entity.User;
import com.back.web7_9_codecrete_be.domain.users.repository.UserRepository;
import com.back.web7_9_codecrete_be.global.error.code.AuthErrorCode;
import com.back.web7_9_codecrete_be.global.error.code.LocationErrorCode;
import com.back.web7_9_codecrete_be.global.error.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LocationService {

    private final LocationRepository locationRepository;
    private final KakaoLocalService kakaoLocalService;


    @Transactional(readOnly = true)
    public LocationResponseDto getLocation(User user){                     //내 위치 불러오기

        Location location =locationRepository.findByUser(user);
        if(location == null){
            throw new BusinessException(LocationErrorCode.LOCATION_NOT_FOUND);
        }

        return LocationResponseDto.from(location);
    }

    @Transactional
    public LocationResponseDto saveLocation(LocationRequestDto locationRequestDto, User user){        //내 위치 저장
                                                                                                                        //예외 처리할 것: 내 위치가 저장되어있는지 확인         //로그인 되어있는지 확인         //추가하면 좋을것? 이게 대한민국의 좌표인지
        validateKoreaCoordinate(locationRequestDto.getLat(), locationRequestDto.getLon());

        if (locationRepository.existsByUser(user)) {
            throw new BusinessException(LocationErrorCode.LOCATION_ALREADY_EXISTS);
        }
        String addressName = kakaoLocalService.coordinateToAddressName(locationRequestDto.getLat(), locationRequestDto.getLon());
        if (addressName == null || addressName.isBlank()) {
            throw new BusinessException(LocationErrorCode.LOCATION_NOT_EXIST_IN_KAKAO);
        }
        Location location = Location.create(user, locationRequestDto.getLat(), locationRequestDto.getLon(), addressName);
        locationRepository.save(location);
        return LocationResponseDto.from(location);
    }

    @Transactional
    public LocationResponseDto modifyLocation(LocationRequestDto locationRequestDto, User user){      //내 위치 수정

        Location location = locationRepository.findByUser(user);
        if(location == null)
            throw new BusinessException(LocationErrorCode.LOCATION_NOT_FOUND);

        String addressName = kakaoLocalService.coordinateToAddressName(locationRequestDto.getLat(), locationRequestDto.getLon());

        if (addressName == null || addressName.isBlank()) {
            throw new BusinessException(LocationErrorCode.LOCATION_NOT_EXIST_IN_KAKAO);
        }

        location.update(locationRequestDto.getLat(), locationRequestDto.getLon(), addressName);
        location.preUpdate();
        return LocationResponseDto.from(location);
    }

    @Transactional
    public void removeLocation(User user){                    //내 위치 삭제
        Location location = locationRepository.findByUser(user);
        if(location == null){
            throw new BusinessException(LocationErrorCode.LOCATION_NOT_FOUND);
        }
        locationRepository.delete(location);
    }

//    public User getUserEntity(Long userId){                 //userId를 이용해서 User 객체 가져오기
//        return userRepository.findById(userId).orElseThrow(
//                () -> new BusinessException(AuthErrorCode.UNAUTHORIZED_USER)
//        );
//    }


    private void validateKoreaCoordinate(double lat, double lon) {
        if (lat < 33.0 || lat > 39.5 || lon < 124.0 || lon > 132.0) {
            throw new BusinessException(LocationErrorCode.INVALID_KOREA_COORDINATE);
        }
    }
}
