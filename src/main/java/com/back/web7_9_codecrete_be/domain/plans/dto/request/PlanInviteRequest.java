package com.back.web7_9_codecrete_be.domain.plans.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Getter
@NoArgsConstructor
public class PlanInviteRequest {
    @NotNull(message = "사용자 ID는 필수입니다.")
    private Long userId;
}