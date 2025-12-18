package com.back.web7_9_codecrete_be.domain.artists.entity;

import com.back.web7_9_codecrete_be.domain.concerts.entity.Concert;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "concert_artist")
public class ConcertArtist {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "concert_artist_id")
    private long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artist_id", nullable = false)
    private Artist artist;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "concert_id", nullable = false)
    private Concert concert;

    public ConcertArtist(Artist artist, Concert concert) {
        this.artist = artist;
        this.concert = concert;
    }
}
