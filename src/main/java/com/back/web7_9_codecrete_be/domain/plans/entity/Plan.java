package com.back.web7_9_codecrete_be.domain.plans.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "plan")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Plan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "concert_id", nullable = false)
    private Long concertId;

    @Column(name = "title", nullable = false, length = 30)
    private String title;

    @Column(name = "date", nullable = false, length = 30)
    private String date;

    @CreationTimestamp
    @Column(name = "created_date", nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @UpdateTimestamp
    @Column(name = "modified_date", nullable = false)
    private LocalDateTime modifiedDate;


    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PlanParticipant> participants = new ArrayList<>();

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Route> routes = new ArrayList<>();

    @Builder
    public Plan(Long concertId, String title, String date) {
        this.concertId = concertId;
        this.title = title;
        this.date = date;
    }

    public void updateTitle(String title) {
        this.title = title;
    }

    public void updateDate(String date) {
        this.date = date;
    }

    public void addParticipant(PlanParticipant participant) {
        this.participants.add(participant);
        participant.setPlan(this);
    }

    public void addRoute(Route route) {
        this.routes.add(route);
        route.setPlan(this);
    }
}
