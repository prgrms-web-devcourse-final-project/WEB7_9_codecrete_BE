package com.back.web7_9_codecrete_be.domain.plans.dto.request;

import com.back.web7_9_codecrete_be.domain.plans.entity.PlanParticipant;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PlanParticipantRoleUpdateRequest {

    @NotNull(message = "역할은 필수입니다.")
    private PlanParticipant.ParticipantRole role;
}
