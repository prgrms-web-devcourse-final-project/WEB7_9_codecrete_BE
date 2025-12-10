package com.back.web7_9_codecrete_be.domain.concerts.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
public class ConcertsTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private ConcertsEntity concertsEntity;

    private LocalDateTime startTime;

    private LocalDateTime endTime;
}
