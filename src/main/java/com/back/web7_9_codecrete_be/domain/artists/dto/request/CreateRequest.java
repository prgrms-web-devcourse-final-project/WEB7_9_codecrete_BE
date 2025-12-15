package com.back.web7_9_codecrete_be.domain.artists.dto.request;

import com.back.web7_9_codecrete_be.domain.artists.entity.Genre;


public record CreateRequest(
        String artistName,
        String artistGroup,
        String artistType,
        Genre genre
) {
}
