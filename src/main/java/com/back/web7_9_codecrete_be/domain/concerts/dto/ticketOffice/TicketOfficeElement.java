package com.back.web7_9_codecrete_be.domain.concerts.dto.ticketOffice;

import com.back.web7_9_codecrete_be.domain.concerts.entity.TicketOffice;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
public class TicketOfficeElement {

    @Schema(description = "예매처 이름입니다.")
    String ticketOfficeName;

    @Schema(description = "예매처 주소입니다.")
    String ticketOfficeUrl;

    public TicketOfficeElement(TicketOffice ticketOffice) {
        this.ticketOfficeName = ticketOffice.getTicketOfficeName();
        this.ticketOfficeUrl = ticketOffice.getTicketOfficeUrl();
    }
}
