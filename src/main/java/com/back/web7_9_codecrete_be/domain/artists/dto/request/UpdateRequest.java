package com.back.web7_9_codecrete_be.domain.artists.dto.request;

public record UpdateRequest(
        String artistName,
        String artistGroup,
        String artistType,
        String genreName
) {
}
