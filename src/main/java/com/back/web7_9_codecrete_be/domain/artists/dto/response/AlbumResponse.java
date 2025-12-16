package com.back.web7_9_codecrete_be.domain.artists.dto.response;

public record AlbumResponse(
        String albumName,
        String releaseDate,
        String albumType,       // album / single / ep
        String imageUrl,
        String spotifyUrl
) {
}
