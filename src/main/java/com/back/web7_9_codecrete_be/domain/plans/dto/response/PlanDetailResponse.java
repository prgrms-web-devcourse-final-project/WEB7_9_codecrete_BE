package com.back.web7_9_codecrete_be.domain.plans.dto.response;

import com.back.web7_9_codecrete_be.domain.plans.entity.PlanParticipant;
import com.back.web7_9_codecrete_be.domain.plans.entity.Schedule;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;


@Getter
@Builder
public class PlanDetailResponse {
    private Long id;
    private Long concertId;
    private Long createdBy;
    private String title;
    private java.time.LocalDate planDate;
    private LocalDateTime createdDate;
    private LocalDateTime modifiedDate;
    private List<ParticipantInfo> participants;
    private List<ScheduleInfo> schedules;
    private Integer totalDuration;

    @Getter
    @Builder
    public static class ParticipantInfo {
        private Long id;
        private Long userId;
        private PlanParticipant.InviteStatus inviteStatus;
        private PlanParticipant.ParticipantRole role;
    }

    @Getter
    @Builder
    public static class ScheduleInfo {
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
        private Double startPlaceLat;
        private Double startPlaceLon;
        private Double endPlaceLat;
        private Double endPlaceLon;
        private Integer distance;
        private Schedule.TransportType transportType;
        private LocalDateTime createdDate;
        private LocalDateTime modifiedDate;
    }
}