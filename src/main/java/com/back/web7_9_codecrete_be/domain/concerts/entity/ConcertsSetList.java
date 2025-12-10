package com.back.web7_9_codecrete_be.domain.concerts.entity;

import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Getter
public class ConcertsSetList {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Concerts concerts;

    private String artist;

    private String song;

    private int length;

    private String spotifyLink;
}
