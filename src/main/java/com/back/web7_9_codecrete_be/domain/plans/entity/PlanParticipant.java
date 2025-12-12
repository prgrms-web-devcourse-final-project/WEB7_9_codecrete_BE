package com.back.web7_9_codecrete_be.domain.plans.entity;

import jakarta.persistence.*;
import lombok.*;


@Entity
@Table(name = "plan_participant")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlanParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

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
    public PlanParticipant(Long userId, Plan plan, InviteStatus inviteStatus, ParticipantRole role) {
        this.userId = userId;
        this.plan = plan;
        this.inviteStatus = inviteStatus;
        this.role = role;
    }

    public void updateInviteStatus(InviteStatus inviteStatus) {
        this.inviteStatus = inviteStatus;
    }

    public void updateRole(ParticipantRole role) {
        this.role = role;
    }

    public enum InviteStatus {
        PENDING,    // 대기 중
        ACCEPTED,   // 수락
        DECLINED    // 거절
    }

    public enum ParticipantRole {
        HOST,       // 주최자
        PARTICIPANT // 참가자
    }
}