package com.back.web7_9_codecrete_be.domain.artists.dto.response;

import com.back.web7_9_codecrete_be.domain.artists.entity.ArtistType;

import java.util.List;

public record ArtistDetailResponse(
        String artistName,
        String artistGroup,
        ArtistType artistType,
        String profileImageUrl,
        long likeCount,
        int totalAlbums,
        double popularityRating,
        String description,
        List<AlbumResponse> albums,
        List<TopTrackResponse> topTracks,
        List<RelatedArtistResponse> relatedArtists
) {
}
