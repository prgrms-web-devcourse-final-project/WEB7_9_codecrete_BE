package com.back.web7_9_codecrete_be.domain.concerts.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@Entity
@RequiredArgsConstructor
@Getter
@Table(name = "concert")
public class Concert {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "concert_id")
    private Long concertId;

    @ManyToOne
    ConcertPlace concertPlace;

    private String name;

    private String content;

    @Column(name = "ticket_time")
    private String ticketTime;

    @Column(name = "ticket_link")
    private String ticketLink;

    @Column(name = "created_date")
    private LocalDateTime createdDate;

    @Column(name = "modified_date")
    private LocalDateTime modifiedDate;

    @Column(nullable = false)
    private int price;

    @Column(name = "api_concert_id")
    private String apiConcertId;

}
