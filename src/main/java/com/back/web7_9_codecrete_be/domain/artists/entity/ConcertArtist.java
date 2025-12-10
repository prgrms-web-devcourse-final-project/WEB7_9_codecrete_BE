package com.back.web7_9_codecrete_be.domain.artists.entity;

import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Getter
@Table(name = "CONCERT_ARTIST")
public class ConcertArtist {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "concert_artist_id")
    private long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Artist artist;

    // TODO : 추후 concert entity 보고 확인 예정 우선 주석 처리
    /*
    @ManyToOne(fetch = FetchType.LAZY)
    private Concert concert;
     */
}
