package com.back.web7_9_codecrete_be.domain.artists.dto.request;

import com.back.web7_9_codecrete_be.domain.artists.entity.ArtistType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

public record UpdateRequest(
        @Size(max = 200, message = "아티스트 이름은 200자를 넘길 수 없습니다.")
        @Schema(description = "아티스트 이름입니다.")
        String artistName,

        @Size(max = 150, message = "아티스트 그룹 이름은 150자를 넘길 수 없습니다.")
        @Schema(description = "아티스트 소속 그룹입니다. 아티스트 이름이 그룹인 경우, null 로 처리됩니다.")
        String artistGroup,

        @Schema(description = "아티스트 타입입니다.", example = "SOLO or GROUP")
        ArtistType artistType,

        @Size(max = 30, message = "장르 이름은 30자를 넘길 수 없습니다.")
        @Schema(description = "장르 이름입니다.")
        String genreName
) {
}
