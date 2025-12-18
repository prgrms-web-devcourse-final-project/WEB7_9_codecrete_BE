package com.back.web7_9_codecrete_be.domain.artists.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record TopTrackResponse(
        @Schema(description = "노래 제목입니다.")
        String trackName,

        @Schema(description = "노래의 Spotify URL 입니다.")
        String spotifyUrl
) {
}
