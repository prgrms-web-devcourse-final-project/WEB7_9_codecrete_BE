package com.back.web7_9_codecrete_be.domain.concerts.dto.KopisApiDto.result;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
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

    public SetResultResponse() {
    }

    public void addConcerts() { this.addedConcerts++; }
    public void addUpdatedConcerts() { this.updatedConcerts++; }
    public void addConcertPlaces() { this.addedConcertPlaces++; }
    public void addUpdatedConcertPlaces() { this.updatedConcertPlaces++; }
    public void addConcertImages() { this.addedConcertImages++; }
    public void addUpdatedConcertImages() { this.updatedConcertImages++; }
    public void addTicketOffice() { this.addedTicketOffice++; }
    public void addUpdatedTicketOffice() { this.updatedTicketOffice++; }

    public void addedConcertImagesAccumulator(int addedConcertImages) {
        this.addedConcertImages += addedConcertImages;
    }

    public void updatedConcertImagesAccumulator(int updatedConcertImages) {
        this.updatedConcertImages += updatedConcertImages;
    }

    public void addedTicketOfficeAccumulator(int addedTicketOffice) {
        this.addedTicketOffice += addedTicketOffice;
    }

    public void updatedTicketOfficesAccumulator(int updatedTicketOffice) {
        this.updatedTicketOffice += updatedTicketOffice;
    }
}
