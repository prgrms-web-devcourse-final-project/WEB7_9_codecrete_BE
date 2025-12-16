package com.back.web7_9_codecrete_be.domain.plans.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ScheduleListResponse {
    private Long planId;
    private List<ScheduleResponse> schedules;
    private Integer totalDuration; // 총 소요 시간 (분)
}
