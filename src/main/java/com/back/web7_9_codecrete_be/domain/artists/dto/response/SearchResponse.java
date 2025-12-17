package com.back.web7_9_codecrete_be.domain.artists.dto.response;

import com.back.web7_9_codecrete_be.domain.artists.entity.Artist;

public record SearchResponse(
        String artistName,
        String artistGroup,
        int likeCount
) {
    public static SearchResponse from(Artist artist) {
        return new SearchResponse(
                artist.getArtistName(),
                artist.getArtistGroup(),
                artist.getLikeCount()
        );
    }
}
