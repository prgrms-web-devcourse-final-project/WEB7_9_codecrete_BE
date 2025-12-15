package com.back.web7_9_codecrete_be.domain.artists.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "genre")
public class Genre {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "genre_name", nullable = false, length = 30)
    private String genreName;

    @Column(name = "genre_group", length = 30)
    private String genreGroup;

    @Column(name = "genre_memo")
    private String genreMemo;

    public Genre(String genreName, String genreGroup, String genreMemo) {
        this.genreName = genreName;
        this.genreGroup = genreGroup;
        this.genreMemo = genreMemo;
    }
}
