package com.back.web7_9_codecrete_be.domain.artists.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Slice;

import java.util.List;

public record ArtistSliceResponse(
        @Schema(description = "아티스트 정보입니다.")
        List<ArtistListResponse> content
) {
    public static ArtistSliceResponse from(Slice<ArtistListResponse> slice) {
        return new ArtistSliceResponse(
                slice.getContent()
        );
    }
}
