package com.back.web7_9_codecrete_be.domain.artists.dto.response;

import com.back.web7_9_codecrete_be.domain.artists.entity.Artist;

public record LikeArtistResponse(
        Long artistId,
        String artistName,
        String nameKo,
        String imageUrl,
        boolean isLiked
) {
    public static LikeArtistResponse from(Artist artist) {
        return new LikeArtistResponse(
                artist.getId(),
                artist.getArtistName(),
                artist.getNameKo(),
                artist.getImageUrl(),
                true
        );
    }
}
