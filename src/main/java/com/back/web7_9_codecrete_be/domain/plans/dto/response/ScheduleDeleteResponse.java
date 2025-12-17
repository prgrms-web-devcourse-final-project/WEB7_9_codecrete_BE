package com.back.web7_9_codecrete_be.domain.plans.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "세부 일정 삭제 응답 DTO")
public class ScheduleDeleteResponse {
    @Schema(description = "삭제된 세부 일정 ID", example = "1")
    private Long scheduleId;
}
