package com.back.web7_9_codecrete_be.domain.plans.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;


@Getter
@NoArgsConstructor
public class PlanUpdateRequest {
    @Size(max = 100, message = "제목은 100자 이하여야 합니다.")
    private String title;

    private LocalDate planDate;
}