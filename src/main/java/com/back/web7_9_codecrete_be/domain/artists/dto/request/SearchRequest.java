package com.back.web7_9_codecrete_be.domain.artists.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SearchRequest(
        @NotBlank(message = "검색어를 입력해주세요")
        @Size(max = 200, message = "아티스트 이름은 200자를 넘길 수 없습니다.")
        String artistName
) {
}
