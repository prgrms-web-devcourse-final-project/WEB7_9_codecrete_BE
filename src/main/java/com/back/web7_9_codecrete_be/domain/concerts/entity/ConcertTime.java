package com.back.web7_9_codecrete_be.domain.concerts.entity;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "concert_time")
public class ConcertTime {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "concert_time_id")
    private Long concertTimeId;

    @ManyToOne
    private Concert concerts;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;
}
