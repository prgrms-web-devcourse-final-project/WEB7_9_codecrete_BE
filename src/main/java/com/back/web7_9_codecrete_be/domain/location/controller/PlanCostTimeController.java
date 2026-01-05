package com.back.web7_9_codecrete_be.domain.location.controller;

import com.back.web7_9_codecrete_be.domain.location.dto.plan.PlanCostTimeRequest;
import com.back.web7_9_codecrete_be.domain.location.dto.plan.PlanCostTimeResponse;
import com.back.web7_9_codecrete_be.domain.location.service.PlanCostTimeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/location/plan")
public class PlanCostTimeController {

    private final PlanCostTimeService planCostTimeService;
    @Operation(
            summary = "이동 수단 기준 예상 비용/시간 계산",
            description = """
                    출발지/도착지 좌표와 이동 수단(CAR, TRANSIT, WALK)을 기준으로
                    예상 이동 비용과 소요 시간을 계산합니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "계산 성공",
                    content = @Content(
                            schema = @Schema(implementation = PlanCostTimeResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 값",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "경로를 찾을 수 없음",
                    content = @Content
            )
    })
    @PostMapping("/costtime")
    public PlanCostTimeResponse getCostTime(@RequestBody PlanCostTimeRequest request) {
        return planCostTimeService.getCostTime(request);
    }
}