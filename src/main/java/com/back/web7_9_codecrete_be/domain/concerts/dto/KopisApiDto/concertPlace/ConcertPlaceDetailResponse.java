package com.back.web7_9_codecrete_be.domain.concerts.dto.KopisApiDto.concertPlace;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JacksonXmlRootElement(localName = "dbs")
public class ConcertPlaceDetailResponse {
    @JacksonXmlProperty(localName = "db")
    @JacksonXmlElementWrapper(useWrapping = false)
    private ConcertPlaceDetailElement concertPlaceDetail;
}
