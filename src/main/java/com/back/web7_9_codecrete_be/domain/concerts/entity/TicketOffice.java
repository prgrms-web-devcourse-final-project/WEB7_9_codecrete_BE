package com.back.web7_9_codecrete_be.domain.concerts.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class TicketOffice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    Concert concert;

    //예매처 명
    @Column(name = "ticket_office_name", nullable = false)
    private String ticketOfficeName;

    @Column(name = "ticket_office_url", nullable = false)
    private String ticketOfficeUrl;

    public TicketOffice(Concert concert, String ticketOfficeName, String ticketOfficeUrl) {
        this.concert = concert;
        this.ticketOfficeName = ticketOfficeName;
        this.ticketOfficeUrl = ticketOfficeUrl;
    }

}
