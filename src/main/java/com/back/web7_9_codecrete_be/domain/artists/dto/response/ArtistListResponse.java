package com.back.web7_9_codecrete_be.domain.artists.dto.response;

import com.back.web7_9_codecrete_be.domain.artists.entity.Artist;

public record ArtistListResponse(
        String artistName,
        String artistGroup,
        String genreName
) {
    public static ArtistListResponse from(Artist artist) {
        return new ArtistListResponse(
                artist.getArtistName(),
                artist.getArtistGroup(),
                artist.getGenre().getGenreName()
        );
    }
}
