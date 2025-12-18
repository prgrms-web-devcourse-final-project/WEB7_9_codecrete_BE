package com.back.web7_9_codecrete_be.domain.concerts.entity;

import com.back.web7_9_codecrete_be.domain.concerts.dto.KopisApiDto.concertPlace.ConcertPlaceDetailElement;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Entity
@Getter
@RequiredArgsConstructor
@Table(name = "concert_place", indexes = @Index(name="idx_api_concert_place_id", columnList = "api_concert_place_id"))
public class ConcertPlace {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "concert_place_id")
    private Long concertPlaceId;

    @Column(name = "place_name")
    private String placeName;

    @Column(nullable = false)
    private String address;

    private String telephone;

    private String placeUrl;

    @Column(nullable = false)
    private double lat;

    @Column(nullable = false)
    private double lon;

    @Column(nullable = false)
    private int seats;

    private boolean hasRestaurant;

    private boolean hasCafe;

    private boolean hasStore;

    private boolean hasPlayroom;

    private boolean hasNursingRoom;

    private boolean hasBarrierFreeParking;

    private boolean hasBarrierFreeRestRoom;

    private boolean hasBarrierFreeRamp;

    private boolean hasBarrierFreeElevator;

    private boolean hasParking;

    @Column(name = "api_concert_place_id")
    private String apiConcertPlaceId;

    public ConcertPlace(String placeName, String address, double lat, double lon, int seats, String apiConcertPlaceId) {
        this.placeName = placeName;
        this.address = address;
        this.lat = lat;
        this.lon = lon;
        this.seats = seats;
        this.apiConcertPlaceId = apiConcertPlaceId;
    }

    public ConcertPlace(ConcertPlaceDetailElement concertPlaceDetailElement) {
        this.placeName = concertPlaceDetailElement.getConcertPlaceName();
        this.address = concertPlaceDetailElement.getConcertPlaceAddress();
        this.telephone = concertPlaceDetailElement.getTelephone();
        this.placeUrl = concertPlaceDetailElement.getConcertPlaceUrl();
        this.lat = Double.parseDouble(concertPlaceDetailElement.getLat());
        this.lon = Double.parseDouble(concertPlaceDetailElement.getLon());
        this.seats = Integer.parseInt(concertPlaceDetailElement.getSeatScale());
        this.apiConcertPlaceId = concertPlaceDetailElement.getConcertPlaceApiId();
        this.hasRestaurant = concertPlaceDetailElement.getRestaurant().equals("Y");
        this.hasCafe = concertPlaceDetailElement.getCafe().equals("Y");
        this.hasStore = concertPlaceDetailElement.getStore().equals("Y");
        this.hasPlayroom = concertPlaceDetailElement.getPlayGround().equals("Y");
        this.hasNursingRoom = concertPlaceDetailElement.getSuyu().equals("Y");
        this.hasBarrierFreeParking = concertPlaceDetailElement.getParkBarrier().equals("Y");
        this.hasBarrierFreeRestRoom = concertPlaceDetailElement.getRestBarrier().equals("Y");
        this.hasBarrierFreeRamp = concertPlaceDetailElement.getRunwBarrier().equals("Y");
        this.hasBarrierFreeElevator = concertPlaceDetailElement.getElevBarrier().equals("Y");
        this.hasParking = concertPlaceDetailElement.getParkingLot().equals("Y");
    }

    @Override
    public String toString() {
        return "ConcertPlace{" +
                "concertPlaceId=" + concertPlaceId +
                ", placeName='" + placeName + '\'' +
                ", address='" + address + '\'' +
                ", lat=" + lat +
                ", lon=" + lon +
                ", seats=" + seats +
                ", apiConcertPlaceId='" + apiConcertPlaceId + '\'' +
                '}';
    }
}
