package com.back.web7_9_codecrete_be.domain.artists.dto.response;

import com.back.web7_9_codecrete_be.domain.artists.entity.Artist;
import io.swagger.v3.oas.annotations.media.Schema;

public record ArtistListResponse(
        @Schema(description = "아티스트 이름입니다.")
        String artistName,

        @Schema(description = "아티스트 소속 그룹입니다. 아티스트 이름이 그룹인 경우, null 로 처리됩니다.")
        String artistGroup,

        @Schema(description = "장르 이름입니다.")
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
