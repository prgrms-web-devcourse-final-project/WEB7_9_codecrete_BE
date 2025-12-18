package com.back.web7_9_codecrete_be.domain.artists.dto.response;

import com.back.web7_9_codecrete_be.domain.concerts.entity.Concert;

import java.time.LocalDate;

public record ConcertListByArtistResponse(
        String concertName,
        LocalDate startDate,
        String concertPlace
) {
    public static ConcertListByArtistResponse from(Concert concert) {
        return new ConcertListByArtistResponse(
                concert.getName(),
                concert.getStartDate(),
                concert.getConcertPlace().getPlaceName()
        );
    }
}
