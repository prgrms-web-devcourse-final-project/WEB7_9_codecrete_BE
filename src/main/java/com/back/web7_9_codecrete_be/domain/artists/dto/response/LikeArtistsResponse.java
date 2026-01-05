package com.back.web7_9_codecrete_be.domain.artists.dto.response;

import com.back.web7_9_codecrete_be.domain.artists.entity.Artist;
import com.back.web7_9_codecrete_be.domain.concerts.entity.Concert;
import io.swagger.v3.oas.annotations.media.Schema;

public record LikeArtistsResponse(
        @Schema(description = "아티스트 아이디입니다.")
        Long id,

        @Schema(description = "아티스트 이름입니다.")
        String artistName,

        @Schema(description = "한국어 기준 아티스트 이름 입니다.")
        String nameKo,

        @Schema(description = "아티스트 프로필 사진 URL 입니다.")
        String imageUrl
) {
        public static LikeArtistsResponse from(Artist artist) {
                return new LikeArtistsResponse(
                        artist.getId(),
                        artist.getArtistName(),
                        artist.getNameKo(),
                        artist.getImageUrl()
                );
        }
}
