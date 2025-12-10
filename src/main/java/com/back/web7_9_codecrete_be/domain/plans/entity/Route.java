package com.back.web7_9_codecrete_be.domain.plans.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "route")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Route {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "route_id")
    private Long routeId;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private Plan plan;

    @Column(name = "start_place_lat", nullable = false)
    private Double startPlaceLat;

    @Column(name = "start_place_lon", nullable = false)
    private Double startPlaceLon;

    @Column(name = "end_place_lat", nullable = false)
    private Double endPlaceLat;

    @Column(name = "end_place_lon", nullable = false)
    private Double endPlaceLon;

    @Column(name = "distance", nullable = false)
    private Integer distance;

    @Column(name = "duration", nullable = false)
    private Integer duration;

    @Enumerated(EnumType.STRING)
    @Column(name = "route_type", nullable = false)
    private RouteType routeType;


    @Builder
    public Route(Plan plan, Double startPlaceLat, Double startPlaceLon,
                 Double endPlaceLat, Double endPlaceLon, Integer distance,
                 Integer duration, RouteType routeType) {
        this.plan = plan;
        this.startPlaceLat = startPlaceLat;
        this.startPlaceLon = startPlaceLon;
        this.endPlaceLat = endPlaceLat;
        this.endPlaceLon = endPlaceLon;
        this.distance = distance;
        this.duration = duration;
        this.routeType = routeType;
    }

    public void updateRoute(Double startPlaceLat, Double startPlaceLon,
                           Double endPlaceLat, Double endPlaceLon,
                           Integer distance, Integer duration, RouteType routeType) {
        this.startPlaceLat = startPlaceLat;
        this.startPlaceLon = startPlaceLon;
        this.endPlaceLat = endPlaceLat;
        this.endPlaceLon = endPlaceLon;
        this.distance = distance;
        this.duration = duration;
        this.routeType = routeType;
    }

    public enum RouteType {
        도보,
        대중교통,
        차
    }
}
