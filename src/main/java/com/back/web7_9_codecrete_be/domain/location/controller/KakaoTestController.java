package com.back.web7_9_codecrete_be.domain.location.controller;

import com.back.web7_9_codecrete_be.domain.location.dto.KakaoLocalResponse;
import com.back.web7_9_codecrete_be.domain.location.service.KakaoLocalService;
import com.back.web7_9_codecrete_be.global.rsData.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Location - Kakao", description = "카카오 로컬 API 연동(주변 음식점 조회, 좌표→주소 변환) 관련 엔드포인트")
@RestController
@RequestMapping("/api/v1/location/kakao")
@RequiredArgsConstructor
public class KakaoTestController {

    private final KakaoLocalService kakaoLocalService;


    @Operation(
            summary = "주변 음식점 조회",
            description = "좌표(서울 시청 근처)로 카카오 로컬에서 주변 음식점을 조회합니다, 좌표는 입력하면 됩니다." +
                    "예시 : http://localhost:8080/api/v1/location/kakao/restaurant?lat=37.5665&lng=126.9780"
    )
    @PostMapping("/restaurant")
    public List<KakaoLocalResponse.Document> KakaoRestaurants(
            @RequestParam double lat,
            @RequestParam double lng
    ){
        return kakaoLocalService.searchNearbyRestaurants(lat, lng);
    }


    @Operation(
            summary = "주변 카페 조회",
            description = "좌표(서울 시청 근처)로 카카오 로컬에서 주변 카페를 조회합니다, 좌표는 입력하면 됩니다." +
                    "예시 : http://localhost:8080/api/v1/location/kakao/cafes?lat=37.5665&lng=126.9780"
    )
    @PostMapping("/cafes")
    public List<KakaoLocalResponse.Document> KakaoCafes(
            @RequestParam double lat,
            @RequestParam double lng
    ){
        return kakaoLocalService.searchNearbyCafes(lat, lng);
    }


    @Operation(
            summary = "좌표를 주소로 변환",
            description = "위도(lat), 경도(lon)를 받아 카카오 API로 주소명(address_name)을 반환합니다."
    )
    @ApiResponse(responseCode = "200", description = "변환 성공",
            content = @Content(schema = @Schema(implementation = RsData.class)))
    @GetMapping("/coord2address")
    public RsData<String> coord2Address(
            @Parameter(description = "위도(latitude)", example = "37.5665", required = true)
            @RequestParam double lat,

            @Parameter(description = "경도(longitude)", example = "126.9780", required = true)
            @RequestParam double lon
    ) {
        String addressName = kakaoLocalService.coordinateToAddressName(lat, lon);
        return RsData.success("좌표를 주소로 변환했습니다.", addressName);
    }
}
