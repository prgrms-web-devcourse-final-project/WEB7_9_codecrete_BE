package com.back.web7_9_codecrete_be.domain.artists.service.spotifyService;

import com.back.web7_9_codecrete_be.domain.artists.entity.ArtistType;
import java.util.List;

// 아티스트 데이터 임시 저장용 클래스

public class ArtistData {
    public final String spotifyId;
    public final String name;
    public final ArtistType artistType;
    public final String imageUrl;
    public final List<String> genres;
    public final Integer popularity; // Spotify 인기도 (0-100)
    public final Integer followers; // 팔로워 수

    public ArtistData(String spotifyId, String name, ArtistType artistType, String imageUrl, 
                     List<String> genres, Integer popularity, Integer followers) {
        this.spotifyId = spotifyId;
        this.name = name;
        this.artistType = artistType;
        this.imageUrl = imageUrl;
        this.genres = genres;
        this.popularity = popularity;
        this.followers = followers;
    }
}

