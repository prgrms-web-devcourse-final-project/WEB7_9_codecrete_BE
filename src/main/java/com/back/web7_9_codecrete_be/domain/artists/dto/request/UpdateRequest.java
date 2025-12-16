package com.back.web7_9_codecrete_be.domain.artists.dto.request;

import com.back.web7_9_codecrete_be.domain.artists.entity.ArtistType;
import jakarta.validation.constraints.Size;

public record UpdateRequest(
        @Size(max = 200, message = "아티스트 이름은 200자를 넘길 수 없습니다.")
        String artistName,

        @Size(max = 150, message = "아티스트 그룹 이름은 150자를 넘길 수 없습니다.")
        String artistGroup,

        ArtistType artistType,

        @Size(max = 30, message = "장르 이름은 30자를 넘길 수 없습니다.")
        String genreName
) {
}
