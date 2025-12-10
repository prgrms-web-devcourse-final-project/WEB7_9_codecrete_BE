package com.back.web7_9_codecrete_be.domain.artists.entity;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "genre_like")
public class GenreLike {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "genre_like_id")
    private long id;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @ManyToOne(fetch = FetchType.LAZY)
    private Genre genre;

    // TODO : 추후 user Entity 보고 확인 예정 우선 주석 처리
    /*
    @ManyToOne(fetch = FetchType.LAZY)
    private User user;
     */
}
