package com.back.web7_9_codecrete_be.domain.concerts.dto.concert;

import com.back.web7_9_codecrete_be.domain.concerts.entity.Concert;
import com.back.web7_9_codecrete_be.domain.concerts.entity.ConcertLike;
import lombok.Getter;

@Getter
public class ConcertLikeResponse {
    private Long concertId;
    private Boolean isLike;

    public ConcertLikeResponse(Concert concert, Boolean isLike) {
        this.concertId = concert.getConcertId();
        this.isLike = isLike;
    }
}
