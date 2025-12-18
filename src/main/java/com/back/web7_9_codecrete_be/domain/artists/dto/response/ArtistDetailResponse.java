package com.back.web7_9_codecrete_be.domain.artists.dto.response;

import com.back.web7_9_codecrete_be.domain.artists.entity.ArtistType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record ArtistDetailResponse(
        @Schema(description = "아티스트 이름입니다.")
        String artistName,

        @Schema(description = "아티스트 소속 그룹입니다. 아티스트 이름이 그룹인 경우, null 로 처리됩니다.")
        String artistGroup,

        @Schema(description = "아티스트 타입입니다.", example = "SOLO or GROUP")
        ArtistType artistType,

        @Schema(description = "아티스트 프로필 이미지 URL 입니다.")
        String profileImageUrl,

        @Schema(description = "받은 총 좋아요 수(찜한 수) 입니다.")
        long likeCount,

        @Schema(description = "아티스트의 총 앨범 수 입니다.")
        int totalAlbums,

        @Schema(description = "아티스트의 인기도 입니다.(Spotify 기준입니다.)")
        double popularityRating,

        @Schema(description = "아티스트에 대한 설명입니다.")
        String description,

        @Schema(description = "아티스트가 발매한 앨범 리스트입니다.")
        List<AlbumResponse> albums,

        @Schema(description = "아티스트의 가장 인기 있는 트랙 상위 10개입니다.")
        List<TopTrackResponse> topTracks,

        @Schema(description = "아티스트와 관련 있는 다른 아티스트 목록입니다.")
        List<RelatedArtistResponse> relatedArtists
) {
}
