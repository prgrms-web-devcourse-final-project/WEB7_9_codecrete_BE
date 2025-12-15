//package com.back.web7_9_codecrete_be.domain.location.service;
//
//import com.back.web7_9_codecrete_be.domain.location.dto.request.LocationModifyRequestDto;
//import com.back.web7_9_codecrete_be.domain.location.dto.request.LocationRequestDto;
//import com.back.web7_9_codecrete_be.domain.location.dto.response.LocationResponseDto;
//import com.back.web7_9_codecrete_be.domain.location.entity.Location;
//import com.back.web7_9_codecrete_be.domain.location.repository.LocationRepository;
//import com.back.web7_9_codecrete_be.domain.users.entity.User;
//import com.back.web7_9_codecrete_be.domain.users.repository.UserRepository;
//import com.back.web7_9_codecrete_be.global.error.code.AuthErrorCode;
//import com.back.web7_9_codecrete_be.global.error.code.LocationErrorCode;
//import com.back.web7_9_codecrete_be.global.error.exception.BusinessException;
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Service;
//
//@Service
//@RequiredArgsConstructor
//public class LocationService {
//
//    private final LocationRepository locationRepository;
//    private final UserRepository userRepository;
//
//    public LocationResponseDto getLocation(LocationRequestDto locationRequestDto, Long userId){                     //내 위치 불러오기
//
//        User user = getUserEntity(userId);
////        Location location = getLocationByUserId(userId);
//        Location location =locationRepository.findByUserId(userId);
//        if(location == null){
//            throw new BusinessException(LocationErrorCode.LOCATION_NOT_FOUND);
//        }
//        return LocationResponseDto
//    }
//
//    public LocationResponseDto saveLocation(LocationRequestDto locationRequestDto, Long locationId){        //내 위치 저장
//                                                                                                                        //예외 처리할 것: 내 위치가 저장되어있는지 확인
//                                                                                                                        //로그인 되어있는지 확인
//                                                                                                                        //추가하면 좋을것? 이게 대한민국의 좌표인지
////        Location location = locationRepository.findByUser(locationId);
////
////        if(location.isPresent()){
////
////        }
//
//    }
//
//
//    public LocationResponseDto modifyLocation(LocationModifyRequestDto locationModifyRequestDto, Long locationId){      //내 위치 수정
//
//        Location location = getLocationEntity(locationId);
//        double changelat = locationModifyRequestDto.getChangelat();
//        double changelon = locationModifyRequestDto.getChangelon();
//        location.setlat(changelat);
//        location.setlon(changelon);
//        locationRepository.save(location);
//    }
//
//    public void removeLoctaion(LocationRequestDto locationRequestDto, Long userId, Long locationId){                    //내 위치 삭제
//        Location location = getLocationEntity(locationId);
//        locationRepository.delete(location);
//    }
//
//
//    public Location getLocationEntity(Long locationId){
//        return locationRepository.findById(locationId).orElseThrow(
//                () -> new BusinessException(LocationErrorCode.LOCATION_NOT_FOUND)
//        );
//    }
//
//    public User getUserEntity(Long userId){
//        return userRepository.findById(userId).orElseThrow(
//                () -> new BusinessException(AuthErrorCode.UNAUTHORIZED_USER)
//        );
//    }
//
//    public Location getLocationByUserId(Long userId){
//        return locationRepository.findById(userId).orElseThrow(
//                () -> new BusinessException(LocationErrorCode.LOCATION_NOT_FOUND)
//        );
//    }
//
//
//}
