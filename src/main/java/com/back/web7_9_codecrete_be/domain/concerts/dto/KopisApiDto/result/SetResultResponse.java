package com.back.web7_9_codecrete_be.domain.concerts.dto.KopisApiDto.result;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
public class SetResultResponse {
    @Schema(description = "추가된 공연 수")
    private int addedConcerts;

    @Schema(description = "갱신된 공연 수")
    private int updatedConcerts;

    @Schema(description = "추가된 공연 장소 수")
    private int addedConcertPlaces;

    @Schema(description = "갱신된 공연 장소 수")
    private int updatedConcertPlaces;

    @Schema(description = "추가된 공연 이미지 수")
    private int addedConcertImages;

    @Schema(description = "갱신된 공연 이미지 수")
    private int updatedConcertImages;

    @Schema(description = "추가된 예매처 사이트 수")
    private int addedTicketOffice;

    @Schema(description = "갱신된 예매처 사이트 수")
    private int updatedTicketOffice;

    public SetResultResponse(int addedConcerts, int updatedConcerts, int addedConcertPlaces, int updatedConcertPlaces, int addedConcertImages, int updatedConcertImages, int addedTicketOffice, int updatedTicketOffice) {
        this.addedConcerts = addedConcerts;
        this.updatedConcerts = updatedConcerts;
        this.addedConcertPlaces = addedConcertPlaces;
        this.updatedConcertPlaces = updatedConcertPlaces;
        this.addedConcertImages = addedConcertImages;
        this.updatedConcertImages = updatedConcertImages;
        this.addedTicketOffice = addedTicketOffice;
        this.updatedTicketOffice = updatedTicketOffice;
    }
}
