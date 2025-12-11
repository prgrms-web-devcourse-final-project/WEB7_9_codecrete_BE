package com.back.web7_9_codecrete_be.domain.concerts.dto.KopisApiDto.concert;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConcertListElement {
    @JacksonXmlProperty(localName = "mt20id")
    private String apiConcertId;

    @JacksonXmlProperty(localName = "prfnm")
    private String concertName;

    @JacksonXmlProperty(localName = "prfpdfrom")
    private String startDate;

    @JacksonXmlProperty(localName = "prfpdto")
    private String endDate;

    @JacksonXmlProperty(localName = "fcltynm")
    private String concertPlace;

    @JacksonXmlProperty(localName = "poster")
    private String posterUrl;

    @JacksonXmlProperty(localName = "area")
    private String area;

    @JacksonXmlProperty(localName = "genrenm")
    private String genreName;

    @JacksonXmlProperty(localName = "openrun")
    private String openrun;

    @JacksonXmlProperty(localName = "prfstate")
    private String concertState;
}
