package com.back.web7_9_codecrete_be.domain.artists.entity;

import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Getter
@Table(name = "ARTIST")
public class Artist {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "artist_id")
    public long id;

    @Column(name = "artist_name", nullable = false, length = 30)
    public String artistName;

    @Column(name = "artist_group", length = 30)
    public String artistGroup;

    @Column(name = "artist_type", nullable = false, length = 20)
    public String artistType;

    @ManyToOne(fetch = FetchType.LAZY)
    private Genre genre;

}
