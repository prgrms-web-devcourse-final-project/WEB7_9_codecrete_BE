package com.back.web7_9_codecrete_be.domain.artists.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "artist")
public class Artist {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "artist_id")
    private long id;

    @Column(name = "artist_name", nullable = false)
    private String artistName;

    @Column(name = "artist_group")
    private String artistGroup;

    @Column(name = "artist_type")
    private String artistType;

    @ManyToOne(fetch = FetchType.LAZY)
    private Genre genre;

    @Column(name = "spotify_artist_id", unique = true)
    private String spotifyArtistId;

    @Column(name = "name_ko", length = 200)
    private String nameKo;

    public Artist(String spotifyArtistId, String artistName, String artistGroup, String artistType, Genre genre) {
        this.spotifyArtistId = spotifyArtistId;
        this.artistName = artistName;
        this.artistGroup = artistGroup; // 옵션 B: seed에서는 null
        this.artistType = artistType;   // 옵션 B: seed에서는 "SINGER"
        this.genre = genre;
    }

    public void updateProfile(String nameKo, String artistGroup, String artistType) {
        this.nameKo = nameKo;
        this.artistGroup = artistGroup;   // nullable
        this.artistType = artistType;     // "SOLO" / "GROUP"
    }


}
