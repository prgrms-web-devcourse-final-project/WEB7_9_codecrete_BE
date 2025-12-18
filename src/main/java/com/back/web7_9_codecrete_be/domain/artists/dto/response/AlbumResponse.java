package com.back.web7_9_codecrete_be.domain.artists.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record AlbumResponse(
        @Schema(description = "앨범 이름입니다.")
        String albumName,

        @Schema(description = "앨범 발매일입니다.", format = "yyyy-MM-dd")
        String releaseDate,

        @Schema(description = "앨범 타입입니다.", example = "album or single or ep")
        String albumType,

        @Schema(description = "앨범 이미지 URL 입니다.")
        String imageUrl,

        @Schema(description = "앨범 Spotify URL 입니다.")
        String spotifyUrl
) {
}
