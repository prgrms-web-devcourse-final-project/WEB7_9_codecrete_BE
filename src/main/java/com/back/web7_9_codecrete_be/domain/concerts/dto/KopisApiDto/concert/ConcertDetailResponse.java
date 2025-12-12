package com.back.web7_9_codecrete_be.domain.concerts.dto.KopisApiDto.concert;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JacksonXmlRootElement(localName = "dbs")
public class ConcertDetailResponse {
    @JacksonXmlProperty(localName = "db")
    private ConcertDetailElement concertDetail;

}
