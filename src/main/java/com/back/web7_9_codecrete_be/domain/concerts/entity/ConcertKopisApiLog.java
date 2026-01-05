package com.back.web7_9_codecrete_be.domain.concerts.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
public class ConcertKopisApiLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @CreationTimestamp
    LocalDateTime createdDate;

    String title;

    String description;

    Long backUpIndex;

    public ConcertKopisApiLog(Long id, LocalDateTime createdDate, String title, String description, Long backUpIndex) {
        this.id = id;
        this.createdDate = createdDate;
        this.title = title;
        this.description = description;
        this.backUpIndex = backUpIndex;
    }
}
