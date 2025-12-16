package com.back.web7_9_codecrete_be.domain.location.controller;

import com.back.web7_9_codecrete_be.domain.location.dto.KakaoLocalResponse;
import com.back.web7_9_codecrete_be.domain.location.service.KakaoLocalService;
import com.back.web7_9_codecrete_be.global.rsData.RsData;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/location/kakao")
@RequiredArgsConstructor
public class KakaoTestController {

    private final KakaoLocalService kakaoLocalService;

    @GetMapping("/restaurants")
    public List<KakaoLocalResponse.Document> testKakaoRestaurants() {

        //테스트용 하드코딩 좌표 (서울 시청 근처)
        double lat = 37.5665;
        double lng = 126.9780;

        return kakaoLocalService.searchNearbyRestaurants(lat, lng);
    }

    @GetMapping("/coord2address")
    public RsData<String> coord2Address(
            @RequestParam double lat,
            @RequestParam double lon
    ) {
        String addressName = kakaoLocalService.coordinateToAddressName(lat, lon);
        return RsData.success("좌표를 주소로 변환했습니다.", addressName);
    }
}
