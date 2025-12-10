package com.back.web7_9_codecrete_be.domain.artists.entity;

import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Getter
@Table(name = "arist")
public class Artist {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "artist_id")
    private long id;

    @Column(name = "artist_name", nullable = false, length = 30)
    private String artistName;

    @Column(name = "artist_group", length = 30)
    private String artistGroup;

    @Column(name = "artist_type", nullable = false, length = 20)
    private String artistType;

    @ManyToOne(fetch = FetchType.LAZY)
    private Genre genre;

}
