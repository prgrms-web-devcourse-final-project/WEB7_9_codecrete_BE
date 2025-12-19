package com.back.web7_9_codecrete_be.domain.plans.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.time.LocalTime;


@Entity
@Table(name = "schedule")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Schedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "schedule_id")
    private Long scheduleId;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private Plan plan;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "schedule_type", nullable = false)
    private ScheduleType scheduleType;

    @Column(name = "title", length = 100, nullable = false)
    private String title;

    @Column(name = "start_at", nullable = false)
    private LocalTime startAt;

    @Column(name = "duration", nullable = false)
    private Integer duration;

    @Column(name = "location", length = 255, nullable = false)
    private String location;

    // 위도/경도는 선택적 (지도 표시가 필요한 경우에만 입력)
    @Column(name = "location_lat")
    private Double locationLat;

    @Column(name = "location_lon")
    private Double locationLon;

    @Column(name = "estimated_cost", nullable = false)
    private Integer estimatedCost;

    @Column(name = "details", columnDefinition = "TEXT", nullable = false)
    private String details;

    // 교통 수단 정보 (schedule_type = 'TRANSPORT'인 경우 사용)
    @Column(name = "start_place_lat")
    private Double startPlaceLat;

    @Column(name = "start_place_lon")
    private Double startPlaceLon;

    @Column(name = "end_place_lat")
    private Double endPlaceLat;

    @Column(name = "end_place_lon")
    private Double endPlaceLon;

    @Column(name = "distance")
    private Integer distance;

    @Enumerated(EnumType.STRING)
    @Column(name = "transport_type")
    private TransportType transportType;

    @CreationTimestamp
    @Column(name = "created_date", nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @UpdateTimestamp
    @Column(name = "modified_date", nullable = false)
    private LocalDateTime modifiedDate;

    // 메인 이벤트(콘서트) 여부 - true인 경우 삭제 불가
    @Column(name = "is_main_event", nullable = false)
    private Boolean isMainEvent = false;


    @Builder
    public Schedule(Plan plan, ScheduleType scheduleType, String title,
                    LocalTime startAt, Integer duration,
                    String location, Double locationLat, Double locationLon,
                    Integer estimatedCost, String details,
                    Double startPlaceLat, Double startPlaceLon,
                    Double endPlaceLat, Double endPlaceLon,
                    Integer distance, TransportType transportType,
                    Boolean isMainEvent) {
        this.plan = plan;
        this.scheduleType = scheduleType;
        this.title = title;
        this.startAt = startAt;
        this.duration = duration;
        this.location = location;
        this.locationLat = locationLat;
        this.locationLon = locationLon;
        this.estimatedCost = estimatedCost;
        this.details = details;
        this.startPlaceLat = startPlaceLat;
        this.startPlaceLon = startPlaceLon;
        this.endPlaceLat = endPlaceLat;
        this.endPlaceLon = endPlaceLon;
        this.distance = distance;
        this.transportType = transportType;
        this.isMainEvent = isMainEvent != null ? isMainEvent : false;
    }

    public void update(ScheduleType scheduleType, String title,
                      LocalTime startAt, Integer duration,
                      String location, Double locationLat, Double locationLon,
                      Integer estimatedCost, String details,
                      Double startPlaceLat, Double startPlaceLon,
                      Double endPlaceLat, Double endPlaceLon,
                      Integer distance, TransportType transportType) {
        this.scheduleType = scheduleType;
        this.title = title;
        this.startAt = startAt;
        this.duration = duration;
        this.location = location;
        this.locationLat = locationLat;
        this.locationLon = locationLon;
        this.estimatedCost = estimatedCost;
        this.details = details;
        this.startPlaceLat = startPlaceLat;
        this.startPlaceLon = startPlaceLon;
        this.endPlaceLat = endPlaceLat;
        this.endPlaceLon = endPlaceLon;
        this.distance = distance;
        this.transportType = transportType;
    }

    public enum ScheduleType {
        TRANSPORT,  // 교통
        MEAL,       // 식사
        WAITING,    // 대기
        ACTIVITY,   // 활동
        OTHER       // 기타
    }

    public enum TransportType {
        WALK,              // 도보
        PUBLIC_TRANSPORT,  // 대중교통
        CAR                // 차
    }
}