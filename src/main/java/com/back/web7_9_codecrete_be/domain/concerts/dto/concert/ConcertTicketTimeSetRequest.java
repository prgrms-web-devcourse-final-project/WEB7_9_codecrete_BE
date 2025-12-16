package com.back.web7_9_codecrete_be.domain.concerts.dto.concert;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ConcertTicketTimeSetRequest {
    private Long  concertId;
    private LocalDateTime ticketTime;
}
