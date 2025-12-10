package com.back.web7_9_codecrete_be.domain.concerts.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Entity
@Getter
@RequiredArgsConstructor
@Table(name = "concert_like")
public class ConcertLike {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "concert_like_id")
    private Long concertLikeId;

    //TODO : 유저 엔티티 생성시 이어주기

    @ManyToOne
    private Concert concert;
}
