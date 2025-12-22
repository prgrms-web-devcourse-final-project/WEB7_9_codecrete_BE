package com.back.web7_9_codecrete_be.domain.plans.entity;

import com.back.web7_9_codecrete_be.domain.concerts.entity.Concert;
import com.back.web7_9_codecrete_be.domain.users.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


@Entity
@Table(name = "plan")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Plan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "plan_id")
    private Long planId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "concert_id", nullable = false)
    private Concert concert;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "plan_date", nullable = false)
    private LocalDate planDate;

    @CreationTimestamp
    @Column(name = "created_date", nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @UpdateTimestamp
    @Column(name = "modified_date", nullable = false)
    private LocalDateTime modifiedDate;

    @Column(name = "share_token", unique = true, length = 36)
    private String shareToken;

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PlanParticipant> participants = new ArrayList<>();

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 20)
    private List<Schedule> schedules = new ArrayList<>();

    @Builder
    public Plan(Concert concert, User user, String title, LocalDate planDate) {
        this.concert = concert;
        this.user = user;
        this.title = title;
        this.planDate = planDate;
    }
    
    // 편의 메서드: userId를 반환 (기존 코드 호환성)
    public Long getUserId() {
        return user != null ? user.getId() : null;
    }

    public void update(String title, LocalDate planDate) {
        this.title = title;
        this.planDate = planDate;
    }

    public void addParticipant(PlanParticipant participant) {
        this.participants.add(participant);
        participant.setPlan(this);
    }

    public void addSchedule(Schedule schedule) {
        this.schedules.add(schedule);
        schedule.setPlan(this);
    }

    public void generateShareToken() {
        this.shareToken = UUID.randomUUID().toString();
    }

    public void clearShareToken() {
        this.shareToken = null;
    }
}