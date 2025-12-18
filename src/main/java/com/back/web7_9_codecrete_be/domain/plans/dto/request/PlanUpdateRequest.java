package com.back.web7_9_codecrete_be.domain.plans.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;


@Getter
@NoArgsConstructor
@Schema(description = "일정 수정 요청 DTO")
public class PlanUpdateRequest {
    @Size(max = 100, message = "제목은 100자 이하여야 합니다.")
    @Schema(description = "일정 제목", example = "수정된 콘서트 관람 일정")
    private String title;

    @FutureOrPresent(message = "날짜는 현재 또는 미래 날짜여야 합니다.")
    @Schema(description = "일정 날짜", example = "2024-12-25", format = "yyyy-MM-dd")
    private LocalDate planDate;
}