package com.back.web7_9_codecrete_be.domain.concerts.entity;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;

@Entity
@Getter
public class ConcertsReviewEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private ConcertsEntity concertsEntity;

    // TODO : 연관 객체 이어주기

    private String title;

    private String content;

    private LocalDateTime createdTime;

    private LocalDateTime modifiedTime;
}
