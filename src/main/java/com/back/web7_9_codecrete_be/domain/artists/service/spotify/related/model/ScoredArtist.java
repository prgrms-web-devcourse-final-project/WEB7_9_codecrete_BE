package com.back.web7_9_codecrete_be.domain.artists.service.spotify.related.model;

import com.back.web7_9_codecrete_be.domain.artists.entity.Artist;

//점수가 매겨진 아티스트 (관련 아티스트 추천용) : 관련 아티스트 추천 로직에서만 사용되는 내부 모델
public class ScoredArtist {
    public final Artist artist;
    public final double score;
    public final int hashValue; // hash 기반 tie-breaker 값

    public ScoredArtist(Artist artist, double score, int hashValue) {
        this.artist = artist;
        this.score = score;
        this.hashValue = hashValue;
    }
}

