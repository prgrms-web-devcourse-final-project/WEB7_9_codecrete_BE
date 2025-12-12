package com.back.web7_9_codecrete_be.domain.concerts.entity;

import jakarta.persistence.*;

@Entity
public class TicketOffice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
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
