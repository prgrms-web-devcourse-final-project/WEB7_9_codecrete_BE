package com.back.web7_9_codecrete_be.domain.plans.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;


@Getter
@Builder
public class PlanListResponse {
    private Long id;
    private Long concertId;
    private Long createdBy;
    private String title;
    private LocalDate planDate;
    private LocalDateTime createdDate;
    private LocalDateTime modifiedDate;
    private Integer scheduleCount; // 일정 항목 개수
    private Integer totalDuration; // 총 소요 시간
}