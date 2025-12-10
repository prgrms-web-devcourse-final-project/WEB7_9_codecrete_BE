package com.back.web7_9_codecrete_be.domain.concerts.entity;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "concert_review")
public class ConcertReview {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "concert_review_id")
    private Long concertReviewId;

    @ManyToOne
    private Concert concert;

    // TODO : 연관 객체 이어주기

    private String title;

    private String content;

    @Column(name = "created_date")
    private LocalDateTime createdDate;

    @Column(name = "modified_date")
    private LocalDateTime modifiedDate;
}
