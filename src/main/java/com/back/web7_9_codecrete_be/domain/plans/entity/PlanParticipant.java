package com.back.web7_9_codecrete_be.domain.plans.entity;

import com.back.web7_9_codecrete_be.domain.users.entity.User;
import jakarta.persistence.*;
import lombok.*;


@Entity
@Table(name = "plan_participant",
        uniqueConstraints = @UniqueConstraint(name = "uk_plan_participant_user_plan", columnNames = {"user_id", "plan_id"}),
        indexes = {
                @Index(name = "idx_plan_participant_user", columnList = "user_id"),
                @Index(name = "idx_plan_participant_plan", columnList = "plan_id")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlanParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "participant_id")
    private Long participantId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private Plan plan;

    @Enumerated(EnumType.STRING)
    @Column(name = "invite_status", nullable = false)
    private InviteStatus inviteStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private ParticipantRole role;

    @Builder
    public PlanParticipant(User user, Plan plan, InviteStatus inviteStatus, ParticipantRole role) {
        this.user = user;
        this.plan = plan;
        this.inviteStatus = inviteStatus;
        this.role = role;
    }
    
    // 편의 메서드: userId를 반환 (기존 코드 호환성)
    public Long getUserId() {
        return user != null ? user.getId() : null;
    }

    public void updateInviteStatus(InviteStatus inviteStatus) {
        this.inviteStatus = inviteStatus;
    }

    public void updateRole(ParticipantRole role) {
        this.role = role;
    }

    public enum InviteStatus {
        JOINED,     // 참가
        PENDING,    // 대기 중
        ACCEPTED,   // 수락
        DECLINED,   // 거절
        LEFT,       // 나가기
        REMOVED     // 강퇴
    }

    public enum ParticipantRole {
        OWNER,      // 소유자
        EDITOR,     // 편집자
        VIEWER      // 뷰어
    }
}