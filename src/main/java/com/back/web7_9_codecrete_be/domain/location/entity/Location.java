package com.back.web7_9_codecrete_be.domain.location.entity;


import com.back.web7_9_codecrete_be.domain.users.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name="location")
@Getter
@NoArgsConstructor
public class Location {

    @Id
    @Column(name = "location_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    // TODO : 추후 user Entity 보고 확인 예정 우선 주석 처리

    @ManyToOne
    @JoinColumn(name="user_id", nullable = false)
    private User user;

    @Column(length = 255)
    private String address;

    @Column(nullable = false)
    private LocalDateTime modified_date;

    private double lat;

    private double lon;


    @PrePersist
    public void prePersist() {
        this.modified_date = LocalDateTime.now(); // ⭐ insert 때도 채움
    }

    @PreUpdate
    public void preUpdate() {
        this.modified_date = LocalDateTime.now();
    }

    protected Location(User user, double lat, double lon, String address) {
        this.user = user;
        this.lat = lat;
        this.lon = lon;
        this.address = address;
    }

    public static Location create(User user, double lat, double lon, String addressName) {
        return new Location(user, lat, lon, addressName);
    }

    public void update(double lat, double lon, String address) {

        this.lat = lat;
        this.lon = lon;
        this.address = address;
    }
}
