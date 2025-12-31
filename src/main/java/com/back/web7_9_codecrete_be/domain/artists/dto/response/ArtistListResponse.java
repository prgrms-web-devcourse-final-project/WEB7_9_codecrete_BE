package com.back.web7_9_codecrete_be.domain.artists.dto.response;

import com.back.web7_9_codecrete_be.domain.artists.entity.Artist;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record ArtistListResponse(
        @Schema(description = "아티스트 아이디입니다.")
        Long id,

        @Schema(description = "아티스트 이름입니다.")
        String artistName,

        @Schema(description = "한국어 기준 아티스트 이름 입니다.")
        String nameKo,

        @Schema(description = "아티스트 소속 그룹입니다. 아티스트 이름이 그룹인 경우, null 로 처리됩니다.")
        String artistGroup,

        @Schema(description = "장르입니다.")
        List<String> genres,

        @Schema(description = "받은 좋아요 수(찜한 사람 수) 입니다.")
        int likeCount,

        @Schema(description = "아티스트 프로필 사진 URL 입니다.")
        String imageUrl,

        @Schema(description = "로그인한 유저의 좋아요 여부입니다. 비회원인 경우 false입니다.")
        Boolean isLiked
) {
    public static List<String> getGenre(Artist artist) {
        return artist.getArtistGenres().stream()
                .map(ag -> ag.getGenre().getGenreName())
                .distinct()
                .toList();
    }

    public static ArtistListResponse from(Artist artist) {
        List<String> genres = getGenre(artist);
        return new ArtistListResponse(
                artist.getId(),
                artist.getArtistName(),
                artist.getNameKo(),
                artist.getArtistGroup(),
                genres,
                artist.getLikeCount(),
                artist.getImageUrl(),
                false // 기본값은 false
        );
    }

    public static ArtistListResponse from(Artist artist, boolean isLiked) {
        List<String> genres = getGenre(artist);
        return new ArtistListResponse(
                artist.getId(),
                artist.getArtistName(),
                artist.getNameKo(),
                artist.getArtistGroup(),
                genres,
                artist.getLikeCount(),
                artist.getImageUrl(),
                isLiked
        );
    }
}
