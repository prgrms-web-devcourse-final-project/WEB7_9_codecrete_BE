package com.back.web7_9_codecrete_be.domain.location.controller;

import com.back.web7_9_codecrete_be.domain.location.dto.fe.TmapWalkFeResponse;
import com.back.web7_9_codecrete_be.domain.location.dto.response.tmap.TmapSummaryAllResponse;
import com.back.web7_9_codecrete_be.domain.location.dto.response.tmap.TmapWalkResponse;
import com.back.web7_9_codecrete_be.domain.location.service.TmapService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(
        name = "Location - Tmap",
        description = "Tmap 대중교통 길찾기 API 연동"
)
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/location")
public class TmapApiController {

    private final TmapService tmapService;

    @Operation(
            summary = "대중교통 경로 조회",
            description = """
            출발지(startX, startY)와 도착지(endX, endY) 좌표를 기반으로  
            Tmap 대중교통 API를 호출하여 경로 정보를 조회합니다.
            """
    )
    @ApiResponse(
            responseCode = "200",
            description = "경로 조회 성공",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(
                            description = "Tmap 대중교통 경로 조회 결과(JSON 문자열)"
                    )
            )
    )
    @GetMapping("/tmap/summary")
    public TmapSummaryAllResponse getSummary(

            @Parameter(
                    description = "출발지 경도 (longitude)",
                    example = "126.9780",
                    required = true
            )
            @RequestParam double startX,

            @Parameter(
                    description = "출발지 위도 (latitude)",
                    example = "37.5665",
                    required = true
            )
            @RequestParam double startY,

            @Parameter(
                    description = "도착지 경도 (longitude)",
                    example = "127.0276",
                    required = true
            )
            @RequestParam double endX,

            @Parameter(
                    description = "도착지 위도 (latitude)",
                    example = "37.4979",
                    required = true
            )
            @RequestParam double endY
    ) {
        return tmapService.getSummaryRoute(startX, startY, endX, endY);
    }
    @GetMapping("/tmap/route")
    public String getTransit(

            @Parameter(
                    description = "출발지 경도 (longitude)",
                    example = "126.9780",
                    required = true
            )
            @RequestParam double startX,

            @Parameter(
                    description = "출발지 위도 (latitude)",
                    example = "37.5665",
                    required = true
            )
            @RequestParam double startY,

            @Parameter(
                    description = "도착지 경도 (longitude)",
                    example = "127.0276",
                    required = true
            )
            @RequestParam double endX,

            @Parameter(
                    description = "도착지 위도 (latitude)",
                    example = "37.4979",
                    required = true
            )
            @RequestParam double endY
    ) {
        return tmapService.getRoute(startX, startY, endX, endY);
    }
    @Operation(
            summary = "Tmap 도보만 경로 요약",
            description = "출발지와 도착지 좌표를 기준으로 Tmap 도보 경로의 총 거리와 소요 시간을 반환합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "도보 경로 요약 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = TmapWalkFeResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "경로를 찾을 수 없음"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Tmap 외부 API 오류"
            )
    })
    @GetMapping("/tmap/walk")
    public TmapWalkFeResponse getTmapFeWalk(
            @RequestParam double startX,
            @RequestParam double startY,
            @RequestParam double endX,
            @RequestParam double endY
    ) {
       return tmapService.getWalkSummary(startX, startY, endX, endY);
    }
}
