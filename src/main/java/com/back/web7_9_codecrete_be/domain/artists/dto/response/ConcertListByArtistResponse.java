package com.back.web7_9_codecrete_be.domain.artists.dto.response;

import com.back.web7_9_codecrete_be.domain.concerts.entity.Concert;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

public record ConcertListByArtistResponse(
        @Schema(description = "공연명 입니다.")
        String concertName,

        @Schema(description = "공연 시작 날짜입니다.", format = "yyyy-MM-dd")
        LocalDate startDate,

        @Schema(description = "공연 장소입니다.")
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
