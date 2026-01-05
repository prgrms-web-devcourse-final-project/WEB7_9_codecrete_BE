package com.back.web7_9_codecrete_be.domain.concerts.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@RequiredArgsConstructor
public class ConcertKopisApiLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @CreationTimestamp
    LocalDateTime createdDate;

    @Column(name ="work_type",nullable = false)
    String workType;

    @Column(name = "status")
    String status;

    @Column(name ="description")
    String description;

    @Column(name = "back_up_index")
    Long backUpIndex;

    public ConcertKopisApiLog(String workType, String status, String description, Long backUpIndex) {
        this.workType  = workType;
        this.description = description;
        this.status = status;
        this.backUpIndex = backUpIndex;
    }
}
