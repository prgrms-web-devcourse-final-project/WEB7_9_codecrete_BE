package com.back.web7_9_codecrete_be.domain.location.controller;

import com.back.web7_9_codecrete_be.domain.location.dto.fe.KakaoRouteFeResponse;
import com.back.web7_9_codecrete_be.domain.location.dto.fe.KakaoRouteSectionFeResponse;
import com.back.web7_9_codecrete_be.domain.location.dto.plan.PlanCostTimeRequest;
import com.back.web7_9_codecrete_be.domain.location.dto.plan.PlanCostTimeResponse;
import com.back.web7_9_codecrete_be.domain.location.dto.response.kakao.KakaoLocalResponse;
import com.back.web7_9_codecrete_be.domain.location.dto.response.kakao.KakaoMobilityResponse;
import com.back.web7_9_codecrete_be.domain.location.dto.request.kakao.KakaoRouteTransitRequest;
import com.back.web7_9_codecrete_be.domain.location.dto.response.kakao.KakaoRouteTransitResponse;
import com.back.web7_9_codecrete_be.domain.location.service.KakaoLocalService;
import com.back.web7_9_codecrete_be.domain.location.service.PlanCostTimeService;
import com.back.web7_9_codecrete_be.domain.plans.entity.Schedule;
import com.back.web7_9_codecrete_be.global.error.code.LocationErrorCode;
import com.back.web7_9_codecrete_be.global.error.exception.BusinessException;
import com.back.web7_9_codecrete_be.global.rsData.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Tag(name = "Location - Kakao", description = "카카오 로컬 API 연동(주변 음식점 조회, 좌표→주소 변환) 관련 엔드포인트")
@RestController
@RequestMapping("/api/v1/location/kakao")
@RequiredArgsConstructor
public class KakaoApiController {

    private final KakaoLocalService kakaoLocalService;


    @Operation(
            summary = "주변 음식점 조회",
            description = "좌표(서울 시청 근처)로 카카오 로컬에서 주변 음식점을 조회합니다, 좌표는 입력하면 됩니다." +
                    "예시 : http://localhost:8080/api/v1/location/kakao/restaurant?lat=37.5665&lon=126.9780"
    )
    @PostMapping("/restaurant")
    public List<KakaoLocalResponse.Document> KakaoRestaurants(
            @RequestParam double x,
            @RequestParam double y
    ) {
        return kakaoLocalService.searchNearbyRestaurants(x, y);
    }


    @Operation(
            summary = "주변 카페 조회",
            description = "좌표(서울 시청 근처)로 카카오 로컬에서 주변 카페를 조회합니다, 좌표는 입력하면 됩니다." +
                    "예시 : http://localhost:8080/api/v1/location/kakao/cafes?lat=37.5665&lon=126.9780"
    )
    @PostMapping("/cafes")
    public List<KakaoLocalResponse.Document> KakaoCafes(
            @RequestParam double x,
            @RequestParam double y
    ) {
        return kakaoLocalService.searchNearbyCafes(x, y);
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

    @Operation(
            summary = "자동차 길찾기 - 안내(Guide) 목록만 조회",
            description = "카카오 모빌리티 자동차 길찾기 결과에서 Guide(안내) 목록만 flatten 해서 반환합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = KakaoMobilityResponse.Guide.class)))
            )
    })
    @GetMapping("/navigate/guides")
    public List<KakaoMobilityResponse.Guide> navigateGuides(
            @Parameter(description = "출발지 경도", example = "126.977969", required = true)
            @RequestParam double startX,

            @Parameter(description = "출발지 위도", example = "37.566535", required = true)
            @RequestParam double startY,

            @Parameter(description = "도착지 경도", example = "126.986037", required = true)
            @RequestParam double endX,

            @Parameter(description = "도착지 위도", example = "37.563617", required = true)
            @RequestParam double endY
    ) {
        KakaoMobilityResponse res = kakaoLocalService.NaviSearch(startX, startY, endX, endY);

        if (res == null || res.getRoutes() == null || res.getRoutes().isEmpty()) {
            return List.of();
        }

        KakaoMobilityResponse.Route route0 = res.getRoutes().get(0);
        if (route0.getSections() == null || route0.getSections().isEmpty()) {
            return List.of();
        }

        return route0.getSections().stream()
                .filter(section -> section.getGuides() != null && !section.getGuides().isEmpty())
                .flatMap(section -> section.getGuides().stream())
                .toList();
    }

    @Operation(
            summary = "자동차 길찾기 - 요약(Summary) 조회",
            description = "카카오 모빌리티 자동차 길찾기 결과에서 0번 route의 summary(총 거리/시간)를 반환합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = KakaoMobilityResponse.Summary.class))
            ),
            @ApiResponse(responseCode = "404", description = "경로를 찾을 수 없음", content = @Content)
    })
    @GetMapping("/navigate/summary")
    public KakaoMobilityResponse.Summary navigateSummary(
            @Parameter(description = "출발지 경도", example = "126.977969", required = true)
            @RequestParam double startX,

            @Parameter(description = "출발지 위도", example = "37.566535", required = true)
            @RequestParam double startY,

            @Parameter(description = "도착지 경도", example = "126.986037", required = true)
            @RequestParam double endX,

            @Parameter(description = "도착지 위도", example = "37.563617", required = true)
            @RequestParam double endY
    ) {
        KakaoMobilityResponse res = kakaoLocalService.NaviSearchSummary(startX, startY, endX, endY);

        if (res == null || res.getRoutes() == null || res.getRoutes().isEmpty()) {
            throw new BusinessException(LocationErrorCode.ROUTE_NOT_FOUND);
        }

        KakaoMobilityResponse.Route route0 = res.getRoutes().get(0);
        if (route0.getSummary() == null) {
            throw new BusinessException(LocationErrorCode.ROUTE_NOT_FOUND);
        }

        return route0.getSummary();

    }

    //카카오 자동차 api인데, 경유지가 존재하는 경우에 사용
    @Operation(
            summary = "자동차 길찾기 - 경유지 포함(구간별) 요약 FE 응답",
            description = "경유지를 포함한 길찾기를 수행하고, 구간별(section) 거리/시간과 from→to 포인트를 FE용으로 재가공해 반환합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = KakaoRouteSectionFeResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "요청값 오류", content = @Content),
            @ApiResponse(responseCode = "404", description = "경로를 찾을 수 없음", content = @Content)
    })
    @PostMapping("/navigate/onlyguide")
    public KakaoRouteSectionFeResponse navigateOnlyGuides(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "경유지 포함 길찾기 요청 바디",
                    required = true,
                    content = @Content(schema = @Schema(implementation = KakaoRouteTransitRequest.class))
            )
            @RequestBody KakaoRouteTransitRequest req
    ) {
        boolean isWayPointsExist = (req.getWaypoints() == null || req.getWaypoints().isEmpty());

        if(!isWayPointsExist){
            KakaoMobilityResponse.Summary summary = navigateSummary(req.getOrigin().getX(), req.getOrigin().getY(), req.getDestination().getX(), req.getDestination().getY());
            List<KakaoRouteFeResponse> sections = List.of(
                    new KakaoRouteFeResponse(
                            0,
                            summary.getDistance(),
                            summary.getDuration(),
                            Map.of("x", req.getOrigin().getX(), "y", req.getOrigin().getY()),
                            Map.of("x", req.getDestination().getX(), "y", req.getDestination().getY())
                    )
            );
            int totalTaxi = 0;
            if (summary.getFare() != null) {
                totalTaxi = summary.getFare().getTaxi();
            }
            return new KakaoRouteSectionFeResponse(
                    summary.getDistance(),
                    summary.getDuration(),
                    totalTaxi,
                    sections

            );
        }
        KakaoRouteTransitResponse res = kakaoLocalService.NaviSearchTransit(req);
        KakaoRouteTransitResponse.Route route = res.getRoutes().get(0);

        KakaoRouteTransitResponse.Summary summary = route.getSummary();

        List<Object> points = new ArrayList<>();        // 출발지, 경유지, 목적지 좌표를 저장

        points.add(summary.getOrigin());
        points.addAll(summary.getWaypoints());      // Waypoints는 배열이니까 addAll 사용
        points.add(summary.getDestination());


        // 구간별 좌표, Distance, Duration 표현
        List<KakaoRouteFeResponse> sections = new ArrayList<>();

        for (int i = 0; i < route.getSections().size(); i++) {
            KakaoRouteTransitResponse.Section section = route.getSections().get(i);

            sections.add(new KakaoRouteFeResponse(          //sections 리스트에 각각의 section 정보들을 추가
                    i,
                    section.getDistance(),
                    section.getDuration(),
                    points.get(i),     //  출발지
                    points.get(i + 1)  //  목적지
            ));
        }

        //  distance전체 합계
        int totalDistance = route.getSections().stream()
                .mapToInt(KakaoRouteTransitResponse.Section::getDistance)
                .sum();

        // duration 전체 합계
        int totalDuration = route.getSections().stream()
                .mapToInt(KakaoRouteTransitResponse.Section::getDuration)
                .sum();
        int totalTaxi = 0;
        if (summary.getFare() != null) {
            totalTaxi = summary.getFare().getTaxi();
        }
        return new KakaoRouteSectionFeResponse(
                totalDistance,
                totalDuration,
                totalTaxi,
                sections
        );
    }
}

