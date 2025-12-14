package com.back.web7_9_codecrete_be.domain.concerts.dto.concert;

import lombok.Getter;

import java.time.LocalDate;

@Getter
public class ConcertUpdateRequest {
    private Long concertId;
    private String name;
    private String description;
    private Long placeId;
    private LocalDate StartDate;
    private LocalDate EndDate;
    private String posterUrl;
    private int maxPrice;
    private int minPrice;

}
