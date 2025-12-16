package com.back.web7_9_codecrete_be.domain.artists.dto.response;

public record RelatedArtistResponse(
        String artistName,
        String imageUrl,
        String spotifyArtistId
) {
}
