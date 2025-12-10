package com.back.web7_9_codecrete_be.domain.concerts.entity;

import jakarta.persistence.*;

@Entity
public class ConcertsSetListEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private ConcertsEntity concertsEntity;

    private String artist;

    private String song;

    private int length;

    private String spotifyLink;
}
