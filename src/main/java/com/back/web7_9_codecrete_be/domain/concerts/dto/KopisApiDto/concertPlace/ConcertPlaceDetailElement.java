package com.back.web7_9_codecrete_be.domain.concerts.dto.KopisApiDto.concertPlace;

import com.back.web7_9_codecrete_be.domain.concerts.entity.ConcertPlace;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ConcertPlaceDetailElement {

    @JacksonXmlProperty(localName = "fcltynm")
    private String concertPlaceName;

    @JacksonXmlProperty(localName = "mt10id")
    private String concertPlaceApiId;

    @JacksonXmlProperty(localName = "mt13cnt")
    private int hallCount;

    @JacksonXmlProperty(localName = "fcltychartr")
    private String concertPlaceType;

    @JacksonXmlProperty(localName = "opende")
    private String openYear;

    @JacksonXmlProperty(localName = "seatscale")
    private String seatScale;

    @JacksonXmlProperty(localName = "telno")
    private String telephone;

    @JacksonXmlProperty(localName = "relateurl")
    private String concertPlaceUrl;

    @JacksonXmlProperty(localName = "adres")
    private String concertPlaceAddress;

    @JacksonXmlProperty(localName = "la")
    private String lat;

    @JacksonXmlProperty(localName = "lo")
    private String lon;

    @JacksonXmlProperty(localName = "restaurant")
    private String restaurant;

    @JacksonXmlProperty(localName = "cafe")
    private String cafe;

    @JacksonXmlProperty(localName = "store")
    private String store;

    @JacksonXmlProperty(localName = "nolibang")
    private String playGround;

    @JacksonXmlProperty(localName = "suyu")
    private String suyu;

    @JacksonXmlProperty(localName = "parkbarrier")
    private String parkBarrier;

    @JacksonXmlProperty(localName = "restbarrier")
    private String restBarrier;

    @JacksonXmlProperty(localName = "runwbarrier")
    private String runwBarrier;

    @JacksonXmlProperty(localName = "elevbarrier")
    private String elevBarrier;

    @JacksonXmlProperty(localName = "parkinglot")
    private String parkingLot;

    @JacksonXmlElementWrapper(localName = "mt13s")
    @JacksonXmlProperty(localName = "mt13")
    private List<ConcertHallResponse> halls;

    public ConcertPlace getConcertPlace() {
        return new ConcertPlace(
                this.concertPlaceName,
                this.concertPlaceAddress,
                Double.parseDouble(lat),
                Double.parseDouble(lon),
                Integer.parseInt(seatScale),
                concertPlaceApiId
                );
    }
}
