package com.back.web7_9_codecrete_be.domain.artists.dto.response;

import com.back.web7_9_codecrete_be.domain.artists.entity.Artist;
import io.swagger.v3.oas.annotations.media.Schema;

public record ArtistListResponse(
        @Schema(description = "아티스트 아이디입니다.")
        Long id,

        @Schema(description = "아티스트 이름입니다.")
        String artistName,

        @Schema(description = "아티스트 소속 그룹입니다. 아티스트 이름이 그룹인 경우, null 로 처리됩니다.")
        String artistGroup,

        @Schema(description = "장르 이름입니다.")
        String genreName,

        @Schema(description = "받은 좋아요 수(찜한 사람 수) 입니다.")
        int likeCount,

        @Schema(description = "아티스트 프로필 사진 URL 입니다.")
        String imageUrl
) {
    public static ArtistListResponse from(Artist artist) {
        return new ArtistListResponse(
                artist.getId(),
                artist.getArtistName(),
                artist.getArtistGroup(),
                artist.getGenre().getGenreName(),
                artist.getLikeCount(),
                artist.getImageUrl()
        );
    }
}
