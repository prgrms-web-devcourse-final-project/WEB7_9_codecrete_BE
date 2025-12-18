package com.back.web7_9_codecrete_be.domain.concerts.dto.concert;

import com.back.web7_9_codecrete_be.domain.concerts.entity.Concert;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
public class ConcertLikeResponse {
    @Schema(description = "공연 ID 입니다.")
    private Long concertId;

    @Schema(description = "공연 좋아요 여부입니다.")
    private Boolean isLike;

    public ConcertLikeResponse(Concert concert, Boolean isLike) {
        this.concertId = concert.getConcertId();
        this.isLike = isLike;
    }
}
