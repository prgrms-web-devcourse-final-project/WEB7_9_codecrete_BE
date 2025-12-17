package com.back.web7_9_codecrete_be.domain.plans.dto.response;

import com.back.web7_9_codecrete_be.domain.plans.entity.Schedule;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Builder
public class ScheduleResponse {
    private Long id;
    private Schedule.ScheduleType scheduleType;
    private String title;
    private LocalTime startAt;
    private Integer duration;
    private String location;
    private Double locationLat;
    private Double locationLon;
    private Integer estimatedCost;
    private String details;
    // 교통 수단 정보
    private Double startPlaceLat;
    private Double startPlaceLon;
    private Double endPlaceLat;
    private Double endPlaceLon;
    private Integer distance;
    private Schedule.TransportType transportType;
    private LocalDateTime createdDate;
    private LocalDateTime modifiedDate;
}
