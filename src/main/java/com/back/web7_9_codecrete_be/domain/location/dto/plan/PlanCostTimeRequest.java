package com.back.web7_9_codecrete_be.domain.location.dto.plan;

import com.back.web7_9_codecrete_be.domain.plans.entity.Schedule;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "이동 수단 기준 예상 비용/시간 계산 요청 DTO")
public class PlanCostTimeRequest {

    @Schema(description = "출발지 경도(longitude)", example = "126.977969", requiredMode = Schema.RequiredMode.REQUIRED)
    private double startX;

    @Schema(description = "출발지 위도(latitude)", example = "37.566535", requiredMode = Schema.RequiredMode.REQUIRED)
    private double startY;

    @Schema(description = "도착지 경도(longitude)", example = "126.986037", requiredMode = Schema.RequiredMode.REQUIRED)
    private double endX;

    @Schema(description = "도착지 위도(latitude)", example = "37.563617", requiredMode = Schema.RequiredMode.REQUIRED)
    private double endY;

    @Schema(
            description = "이동 수단 타입",
            example = "CAR",
            allowableValues = {"CAR", "TRANSIT", "WALK"}
    )
    private Schedule.TransportType transportType;
}
