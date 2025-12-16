package com.back.web7_9_codecrete_be.domain.artists.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateRequest(
        @NotNull(message = "아티스트 이름은 필수로 입력해야합니다.")
        @Size(max = 200, message = "아티스트 이름은 200자를 넘길 수 없습니다.")
        String artistName,

        @Size(max = 150, message = "아티스트 그룹 이름은 150자를 넘길 수 없습니다.")
        String artistGroup,

        @NotNull(message = "아티스트 타입은 필수로 입력해야합니다(SOLO or GROUP)")
        String artistType,

        @NotNull(message = "장르는 필수로 입력해야합니다.")
        @Size(max = 30, message = "장르는 30자를 넘길 수 없습니다.")
        String genreName
) {
}
