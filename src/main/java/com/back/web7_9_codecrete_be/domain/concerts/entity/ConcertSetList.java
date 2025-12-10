package com.back.web7_9_codecrete_be.domain.concerts.entity;

import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Getter
@Table(name = "concert_set_list")
public class ConcertSetList {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Concert concerts;

    private String artist;

    private String song;

    private int length;

    @Column(name = "spotify_link")
    private String spotifyLink;
}
