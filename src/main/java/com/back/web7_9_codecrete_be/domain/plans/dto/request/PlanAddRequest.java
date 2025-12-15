package com.back.web7_9_codecrete_be.domain.plans.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;


@Getter
@NoArgsConstructor
public class PlanAddRequest {
    @NotNull(message = "콘서트 ID는 필수입니다.")
    private Long concertId;

    @NotBlank(message = "제목은 필수입니다.")
    @Size(max = 100, message = "제목은 100자 이하여야 합니다.")
    private String title;

    @NotNull(message = "날짜는 필수입니다.")
    private LocalDate planDate;
}