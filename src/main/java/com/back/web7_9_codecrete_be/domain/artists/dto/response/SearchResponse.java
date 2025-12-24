package com.back.web7_9_codecrete_be.domain.artists.dto.response;

import com.back.web7_9_codecrete_be.domain.artists.entity.Artist;
import io.swagger.v3.oas.annotations.media.Schema;

public record SearchResponse(
        @Schema(description = "아티스트 이름입니다.")
        String artistName,

        @Schema(description = "한국어 기준 아티스트 이름입니다.")
        String nameKo,

        @Schema(description = "아티스트 소속 그룹입니다. 아티스트 이름이 그룹인 경우, null 로 처리됩니다.")
        String artistGroup,

        @Schema(description = "받은 총 좋아요 수(찜한 수) 입니다.")
        int likeCount
) {
    public static SearchResponse from(Artist artist) {
        return new SearchResponse(
                artist.getArtistName(),
                artist.getNameKo(),
                artist.getArtistGroup(),
                artist.getLikeCount()
        );
    }
}
