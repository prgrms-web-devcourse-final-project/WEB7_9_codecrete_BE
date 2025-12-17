package com.back.web7_9_codecrete_be.domain.plans.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@Schema(description = "세부 일정 목록 조회 응답 DTO")
public class ScheduleListResponse {
    @Schema(description = "일정 ID", example = "1")
    private Long planId;
    @Schema(description = "세부 일정 목록")
    private List<ScheduleResponse> schedules;
    @Schema(description = "총 소요 시간(분)", example = "240")
    private Integer totalDuration; // 총 소요 시간 (분)
}
