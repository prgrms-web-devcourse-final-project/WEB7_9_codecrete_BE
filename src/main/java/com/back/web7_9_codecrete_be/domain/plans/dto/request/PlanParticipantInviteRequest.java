package com.back.web7_9_codecrete_be.domain.plans.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Getter
@NoArgsConstructor
@Schema(description = "일정 참여자 초대 요청 DTO")
public class PlanParticipantInviteRequest {
    @NotNull(message = "사용자 ID는 필수입니다.")
    @Schema(description = "초대할 사용자 ID", example = "1")
    private Long userId;
}
