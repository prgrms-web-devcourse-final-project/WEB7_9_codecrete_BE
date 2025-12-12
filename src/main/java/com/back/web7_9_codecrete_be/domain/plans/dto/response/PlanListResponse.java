package com.back.web7_9_codecrete_be.domain.plans.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;


@Getter
@Builder
public class PlanListResponse {
    private Long id;
    private Long concertId;
    private String title;
    private String date;
    private LocalDateTime createdDate;
    private LocalDateTime modifiedDate;
}