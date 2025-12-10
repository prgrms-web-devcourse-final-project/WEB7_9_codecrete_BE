package com.back.web7_9_codecrete_be.domain.concerts.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@Entity
@RequiredArgsConstructor
@Getter
public class Concerts {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long concertsId;

    private String name;

    private String content;

    private String ticketTime;

    private String ticketLink;

    private LocalDateTime createdDate;

    private LocalDateTime modifiedDate;

    private int price;
}
