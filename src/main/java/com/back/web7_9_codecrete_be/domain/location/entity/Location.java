package com.back.web7_9_codecrete_be.domain.location.entity;


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
    @Column(nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name="user_id", nullable = false)
    private User user;

    @Column(length = 255)
    private String address;

    @Column(nullable = false)
    private LocalDateTime modified_date;

    private double lat;

    private double lon;
}
