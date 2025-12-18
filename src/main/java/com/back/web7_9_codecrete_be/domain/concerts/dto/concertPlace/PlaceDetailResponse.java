package com.back.web7_9_codecrete_be.domain.concerts.dto.concertPlace;

import com.back.web7_9_codecrete_be.domain.concerts.entity.ConcertPlace;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
public class PlaceDetailResponse {
    @Schema(description = "공연장 이름입니다.")
    private String placeName;

    @Schema(description = "공연장 주소입니다.")
    private String placeAddress;

    @Schema(description = "공연장 전화번호입니다.")
    private String telephone;

    @Schema(description = "공연장 페이지 주소입니다.")
    private String placeUrl;

    @Schema(description = "공연장 위도입니다.")
    private double lat;

    @Schema(description = "공연장 경도입니다.")
    private double lon;

    @Schema(description = "해당 공연장에 식당이 있는지 여부입니다.")
    private boolean hasRestaurant;

    @Schema(description = "해당 공연장에 카페가 있는지 여부입니다.")
    private boolean hasCafe;

    @Schema(description = "해당 공연장에 편의점이 있는지 여부입니다.")
    private boolean hasStore;

    @Schema(description = "해당 공연장에 놀이방이 있는지 여부입니다.")
    private boolean hasPlayroom;

    @Schema(description = "해당 공연장에 수유실이 있는지 여부입니다.")
    private boolean hasNursingRoom;

    @Schema(description = "해당 공연장에 장애인 전용 주차 시설이 있는지 여부입니다.")
    private boolean hasBarrierFreeParking;

    @Schema(description = "해당 공연장에 장애인 전용 화장실이 있는지 여부입니다.")
    private boolean hasBarrierFreeRestRoom;

    @Schema(description = "해당 공연장에 장애인 전용 경사로가 있는지 여부입니다.")
    private boolean hasBarrierFreeRamp;

    @Schema(description = "해당 공연장에 엘레베이터가 있는지 여부입니다.")
    private boolean hasElevator;

    @Schema(description = "해당 공연장에 주차장이 존재하는지 여부입니다.")
    private boolean hasParking;

    public PlaceDetailResponse(ConcertPlace concertPlace) {
        this.placeName = concertPlace.getPlaceName();
        this.placeAddress = concertPlace.getAddress();
        this.telephone = concertPlace.getTelephone();
        this.placeUrl = concertPlace.getPlaceUrl();
        this.lat = concertPlace.getLat();
        this.lon = concertPlace.getLon();
        this.hasRestaurant = concertPlace.isHasRestaurant();
        this.hasCafe = concertPlace.isHasCafe();
        this.hasStore = concertPlace.isHasStore();
        this.hasPlayroom = concertPlace.isHasPlayroom();
        this.hasNursingRoom = concertPlace.isHasNursingRoom();
        this.hasBarrierFreeParking = concertPlace.isHasBarrierFreeParking();
        this.hasBarrierFreeRestRoom = concertPlace.isHasBarrierFreeRamp();
        this.hasBarrierFreeRamp = concertPlace.isHasBarrierFreeRamp();
        this.hasElevator = concertPlace.isHasElevator();
        this.hasParking = concertPlace.isHasParking();
    }
}
