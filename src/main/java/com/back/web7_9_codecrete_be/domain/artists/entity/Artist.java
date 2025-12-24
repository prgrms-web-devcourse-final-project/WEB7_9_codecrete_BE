package com.back.web7_9_codecrete_be.domain.artists.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;


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

    @Enumerated(EnumType.STRING)
    @Column(name = "artist_type")
    private ArtistType artistType;

    @Column(name = "spotify_artist_id", unique = true)
    private String spotifyArtistId;

    @Column(name = "musicbrainz_id")
    private String musicBrainzId;

    @Column(name = "name_ko", length = 200)
    private String nameKo;

    @Column(name = "like_count", nullable = false)
    private int likeCount = 0;

    @Column(name = "image_url")
    private String imageUrl;

    @OneToMany(mappedBy = "artist", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ArtistGenre> artistGenres = new HashSet<>();

    public Artist(String spotifyArtistId, String artistName, String artistGroup, ArtistType artistType, Genre genre) {
        this.spotifyArtistId = spotifyArtistId;
        this.artistName = artistName;
        this.artistGroup = artistGroup; // 옵션 B: seed에서는 null
        this.artistType = artistType;   // 옵션 B: seed에서는 "SINGER"
        if (genre != null) {
            addGenre(genre);
        }
    }

    // 장르 없이 생성하는 생성자 (시드 데이터용)
    public Artist(String spotifyArtistId, String artistName, String artistGroup, ArtistType artistType) {
        this.spotifyArtistId = spotifyArtistId;
        this.artistName = artistName;
        this.artistGroup = artistGroup;
        this.artistType = artistType;
    }

    public void updateProfile(String nameKo, String artistGroup, ArtistType artistType) {
        this.nameKo = nameKo;
        this.artistGroup = artistGroup;   // nullable
        this.artistType = artistType;     // "SOLO" / "GROUP"
    }

    public void changeName(String name) {
        this.artistName = name;
    }

    public void changeGroup(String group) {
        this.artistGroup = group;
    }

    public void changeType(ArtistType type) {
        this.artistType = type;
    }

    public void increaseLikeCount() {
        this.likeCount++;
    }

    public void decreaseLikeCount() {
        if (this.likeCount > 0) {
            this.likeCount--;
        }
    }

    public void setMusicBrainzId(String musicBrainzId) {
        this.musicBrainzId = musicBrainzId;
    }

    public void addGenre(Genre genre) {
        this.artistGenres.add(new ArtistGenre(this, genre));
    }

    public void replaceGenres(Set<Genre> genres) {
        this.artistGenres.clear(); // orphanRemoval=true라 매핑 row 삭제됨
        for (Genre genre : genres) {
            this.artistGenres.add(new ArtistGenre(this, genre));
        }
    }

}
