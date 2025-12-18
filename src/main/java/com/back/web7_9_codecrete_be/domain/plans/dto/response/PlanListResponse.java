package com.back.web7_9_codecrete_be.domain.plans.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;


@Getter
@Builder
@Schema(description = "일정 목록 조회 응답 DTO")
public class PlanListResponse {
    @Schema(description = "일정 ID", example = "1")
    private Long id;
    @Schema(description = "콘서트 ID", example = "1")
    private Long concertId;
    @Schema(description = "일정 생성자 ID", example = "1")
    private Long createdBy;
    @Schema(description = "일정 제목", example = "콘서트 관람 일정")
    private String title;
    @Schema(description = "일정 날짜", example = "2024-12-25", format = "yyyy-MM-dd")
    private LocalDate planDate;
    @Schema(description = "생성 일시", example = "2024-12-01T10:00:00")
    private LocalDateTime createdDate;
    @Schema(description = "수정 일시", example = "2024-12-01T10:00:00")
    private LocalDateTime modifiedDate;
    @Schema(description = "세부 일정 항목 개수", example = "5")
    private Integer scheduleCount; // 일정 항목 개수
    @Schema(description = "총 소요 시간(분)", example = "240")
    private Integer totalDuration; // 총 소요 시간
}