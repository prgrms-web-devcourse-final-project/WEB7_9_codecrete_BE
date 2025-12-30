package com.back.web7_9_codecrete_be.domain.artists.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record RelatedArtistResponse(
        @Schema(description = "아티스트 id 입니다.")
        Long id,

        @Schema(description = "아티스트 이름입니다.")
        String artistName,

        @Schema(description = "한국어 기준 아티스트 이름입니다.")
        String nameKo,

        @Schema(description = "아티스트 사진 URL 입니다.")
        String imageUrl,

        @Schema(description = "아티스트의 Spotify id 입니다.")
        String spotifyArtistId
) {
}
