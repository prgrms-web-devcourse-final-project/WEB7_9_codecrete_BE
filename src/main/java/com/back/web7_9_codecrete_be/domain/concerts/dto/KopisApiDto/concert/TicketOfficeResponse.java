package com.back.web7_9_codecrete_be.domain.concerts.dto.KopisApiDto.concert;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TicketOfficeResponse {
    @JacksonXmlProperty(localName = "relatenm")
    private String ticketOfficeName;

    @JacksonXmlProperty(localName = "relateurl")
    private String ticketOfficeUrl;

    @Override
    public String toString() {
        return "TicketOffice{" +
                "ticketOfficeName='" + ticketOfficeName + '\'' +
                ", ticketOfficeUrl='" + ticketOfficeUrl + '\'' +
                '}';
    }
}
