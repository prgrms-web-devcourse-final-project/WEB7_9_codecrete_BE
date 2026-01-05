package com.back.web7_9_codecrete_be.domain.location.dto.plan;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "이동 수단 기준 예상 비용/시간 응답 DTO")
public class PlanCostTimeResponse {

    @Schema(description = "예상 비용 (원)", example = "12000")
    private Integer cost;

    @Schema(description = "예상 소요 시간 (분)", example = "35")
    private Integer time;
}
