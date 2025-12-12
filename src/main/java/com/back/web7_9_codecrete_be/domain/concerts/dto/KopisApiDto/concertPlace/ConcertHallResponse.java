package com.back.web7_9_codecrete_be.domain.concerts.dto.KopisApiDto.concertPlace;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConcertHallResponse {
    @JacksonXmlProperty(localName = "prfplcnm")
    private String hallName;

    @JacksonXmlProperty(localName = "mt13id")
    private String hallId;

    @JacksonXmlProperty(localName = "seatscale")
    private String seatScale;

    @JacksonXmlProperty(localName = "stageorchat")
    private String stageOrChat;

    @JacksonXmlProperty(localName = "stagepracat")
    private String stagePracticeAvailable;

    @JacksonXmlProperty(localName = "stagedresat")
    private String stageDressingAvailable;

    @JacksonXmlProperty(localName = "stageoutdrat")
    private String stageOutdoorAvailable;

    @JacksonXmlProperty(localName = "disabledseatscale")
    private String disabledSeatScale;

    @JacksonXmlProperty(localName = "stagearea")
    private String stageArea;
}
