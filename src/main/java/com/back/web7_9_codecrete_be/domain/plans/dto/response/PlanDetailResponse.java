package com.back.web7_9_codecrete_be.domain.plans.dto.response;

import com.back.web7_9_codecrete_be.domain.plans.entity.PlanParticipant;
import com.back.web7_9_codecrete_be.domain.plans.entity.Route;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;


@Getter
@Builder
public class PlanDetailResponse {
    private Long id;
    private Long concertId;
    private String title;
    private String date;
    private LocalDateTime createdDate;
    private LocalDateTime modifiedDate;
    private List<ParticipantInfo> participants;
    private List<RouteInfo> routes;

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
    public static class RouteInfo {
        private Long routeId;
        private Double startPlaceLat;
        private Double startPlaceLon;
        private Double endPlaceLat;
        private Double endPlaceLon;
        private Integer distance;
        private Integer duration;
        private Route.RouteType routeType;
    }
}