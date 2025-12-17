package com.back.web7_9_codecrete_be.domain.location.controller;

import com.back.web7_9_codecrete_be.domain.location.service.TmapService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
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
    @GetMapping("/tmap/transit")
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
}
