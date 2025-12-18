package com.back.web7_9_codecrete_be.domain.plans.dto.response;

import com.back.web7_9_codecrete_be.domain.plans.entity.PlanParticipant;
import com.back.web7_9_codecrete_be.domain.plans.entity.Schedule;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;


@Getter
@Builder
@Schema(description = "일정 상세 조회 응답 DTO")
public class PlanDetailResponse {
    @Schema(description = "일정 ID", example = "1")
    private Long id;
    @Schema(description = "콘서트 ID", example = "1")
    private Long concertId;
    @Schema(description = "일정 생성자 ID", example = "1")
    private Long createdBy;
    @Schema(description = "일정 제목", example = "콘서트 관람 일정")
    private String title;
    @Schema(description = "일정 날짜", example = "2024-12-25", format = "yyyy-MM-dd")
    private java.time.LocalDate planDate;
    @Schema(description = "생성 일시", example = "2024-12-01T10:00:00")
    private LocalDateTime createdDate;
    @Schema(description = "수정 일시", example = "2024-12-01T10:00:00")
    private LocalDateTime modifiedDate;
    @Schema(description = "참여자 목록")
    private List<ParticipantInfo> participants;
    @Schema(description = "세부 일정 목록")
    private List<ScheduleInfo> schedules;
    @Schema(description = "총 소요 시간(분)", example = "240")
    private Integer totalDuration;

    @Getter
    @Builder
    @Schema(description = "참여자 정보")
    public static class ParticipantInfo {
        @Schema(description = "참여자 정보 ID", example = "1")
        private Long id;
        @Schema(description = "사용자 ID", example = "1")
        private Long userId;
        @Schema(description = "초대 상태", example = "ACCEPTED")
        private PlanParticipant.InviteStatus inviteStatus;
        @Schema(description = "참여자 역할", example = "MEMBER")
        private PlanParticipant.ParticipantRole role;
    }

    @Getter
    @Builder
    @Schema(description = "세부 일정 정보")
    public static class ScheduleInfo {
        @Schema(description = "세부 일정 ID", example = "1")
        private Long id;
        @Schema(description = "일정 타입", example = "ACTIVITY")
        private Schedule.ScheduleType scheduleType;
        @Schema(description = "세부 일정 제목", example = "콘서트장 도착")
        private String title;
        @Schema(description = "시작 시간", example = "18:00", format = "HH:mm")
        private LocalTime startAt;
        @Schema(description = "소요 시간(분)", example = "120")
        private Integer duration;
        @Schema(description = "위치", example = "올림픽공원 올림픽홀")
        private String location;
        @Schema(description = "위치 위도", example = "37.5219")
        private Double locationLat;
        @Schema(description = "위치 경도", example = "127.1234")
        private Double locationLon;
        @Schema(description = "예상 비용(원)", example = "150000")
        private Integer estimatedCost;
        @Schema(description = "상세 정보", example = "콘서트장 입장 및 좌석 확인")
        private String details;
        @Schema(description = "출발지 위도 (TRANSPORT 타입일 때)", example = "37.5219")
        private Double startPlaceLat;
        @Schema(description = "출발지 경도 (TRANSPORT 타입일 때)", example = "127.1234")
        private Double startPlaceLon;
        @Schema(description = "도착지 위도 (TRANSPORT 타입일 때)", example = "37.5319")
        private Double endPlaceLat;
        @Schema(description = "도착지 경도 (TRANSPORT 타입일 때)", example = "127.1334")
        private Double endPlaceLon;
        @Schema(description = "거리(미터) (TRANSPORT 타입일 때)", example = "5000")
        private Integer distance;
        @Schema(description = "교통 수단 타입 (TRANSPORT 타입일 때)", example = "SUBWAY")
        private Schedule.TransportType transportType;
        @Schema(description = "생성 일시", example = "2024-12-01T10:00:00")
        private LocalDateTime createdDate;
        @Schema(description = "수정 일시", example = "2024-12-01T10:00:00")
        private LocalDateTime modifiedDate;
    }
}