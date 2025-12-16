package com.back.web7_9_codecrete_be.domain.artists.dto.request;

import jakarta.validation.constraints.NotBlank;

public record SearchRequest(
        @NotBlank(message = "검색어를 입력해주세요")
        String artistName
) {
}
