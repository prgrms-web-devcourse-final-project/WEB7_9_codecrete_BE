package com.back.web7_9_codecrete_be.domain.concerts.entity;

import com.back.web7_9_codecrete_be.domain.users.entity.User;
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

    @ManyToOne
    private User user;

    private String title;

    private String content;

    @Column(name = "created_date")
    private LocalDateTime createdDate;

    @Column(name = "modified_date")
    private LocalDateTime modifiedDate;
}
