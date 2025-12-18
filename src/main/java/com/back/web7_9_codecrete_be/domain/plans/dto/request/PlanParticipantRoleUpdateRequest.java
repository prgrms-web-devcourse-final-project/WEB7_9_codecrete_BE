package com.back.web7_9_codecrete_be.domain.plans.dto.request;

import com.back.web7_9_codecrete_be.domain.plans.entity.PlanParticipant;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "일정 참여자 역할 변경 요청 DTO")
public class PlanParticipantRoleUpdateRequest {

    @NotNull(message = "역할은 필수입니다.")
    @Schema(description = "변경할 참여자 역할", example = "MEMBER")
    private PlanParticipant.ParticipantRole role;
}
