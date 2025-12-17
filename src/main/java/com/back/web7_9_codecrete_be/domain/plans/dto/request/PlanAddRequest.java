package com.back.web7_9_codecrete_be.domain.plans.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;


@Getter
@NoArgsConstructor
@Schema(description = "일정 추가 요청 DTO")
public class PlanAddRequest {
    @NotNull(message = "콘서트 ID는 필수입니다.")
    @Schema(description = "콘서트 ID", example = "1")
    private Long concertId;

    @NotBlank(message = "제목은 필수입니다.")
    @Size(max = 100, message = "제목은 100자 이하여야 합니다.")
    @Schema(description = "일정 제목", example = "콘서트 관람 일정")
    private String title;

    @NotNull(message = "날짜는 필수입니다.")
    @FutureOrPresent(message = "날짜는 현재 또는 미래 날짜여야 합니다.")
    @Schema(description = "일정 날짜", example = "2024-12-25", format = "yyyy-MM-dd")
    private LocalDate planDate;
}