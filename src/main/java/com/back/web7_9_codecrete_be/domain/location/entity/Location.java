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

    public double setlat(double lat){
        this.lat = lat;
        return lat;
    }

    public double setlon(double lon){
        this.lon = lon;
        return lon;
    }
}
