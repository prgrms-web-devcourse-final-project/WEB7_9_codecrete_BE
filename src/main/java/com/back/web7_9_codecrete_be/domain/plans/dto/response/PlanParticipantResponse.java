package com.back.web7_9_codecrete_be.domain.plans.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;


@Getter
@Builder
@Schema(description = "일정 참여자 응답 DTO")
public class PlanParticipantResponse {
    @Schema(description = "일정 ID", example = "1")
    private Long planId;
    @Schema(description = "사용자 ID", example = "1")
    private Long userId;
}