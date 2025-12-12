package com.back.web7_9_codecrete_be.domain.plans.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Getter
@NoArgsConstructor
public class PlanAddRequest {
    @NotBlank(message = "제목은 필수입니다.")
    @Size(max = 30, message = "제목은 30자 이하여야 합니다.")
    private String title;

    @NotBlank(message = "날짜는 필수입니다.")
    @Size(max = 30, message = "날짜는 30자 이하여야 합니다.")
    private String date;
}