package com.back.web7_9_codecrete_be.domain.concerts.entity;

import jakarta.persistence.*;

@Entity
public class ConcertsLikeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //TODO : 유저 엔티티 생성시 이어주기

    @ManyToOne
    private ConcertsEntity concertsEntity;
}
