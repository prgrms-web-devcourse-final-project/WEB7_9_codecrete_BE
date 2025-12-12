package com.back.web7_9_codecrete_be.domain.plans.dto.response;

import lombok.Builder;
import lombok.Getter;


@Getter
@Builder
public class PlanParticipantResponse {
    private Long planId;
    private Long userId;
}