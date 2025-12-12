package com.back.web7_9_codecrete_be.domain.concerts.dto.KopisApiDto.concertPlace;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConcertPlaceListElement {
    @JacksonXmlProperty(localName = "fcltynm")
    private String concertPlaceName;

    @JacksonXmlProperty(localName = "mt10id")
    private String concertPlaceApiId;

    @JacksonXmlProperty(localName = "mt13cnt")
    private Integer concertHallCnt;

    @JacksonXmlProperty(localName = "fcltychartr")
    private String concertPlaceType;

    @JacksonXmlProperty(localName = "sidonm")
    private String sidonm;

    @JacksonXmlProperty(localName = "gugunnm")
    private String gugunnm;

    @JacksonXmlProperty(localName = "opende")
    private String opende;
}
