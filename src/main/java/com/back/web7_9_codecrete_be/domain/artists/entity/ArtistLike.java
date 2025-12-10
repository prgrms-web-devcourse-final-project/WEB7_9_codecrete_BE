package com.back.web7_9_codecrete_be.domain.artists.entity;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "ARTIST_LIKE")
public class ArtistLike {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "artist_like_id")
    private long id;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    // TODO : 추후 user Entity 보고 확인 예정 우선 주석 처리
    /*
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "genre_id", nullable = false)
    private User user;
     */

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artist_id", nullable = false)
    private Artist artist;
}
