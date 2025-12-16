package com.back.web7_9_codecrete_be.domain.concerts.entity;

import com.back.web7_9_codecrete_be.domain.concerts.dto.concert.ConcertUpdateRequest;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@RequiredArgsConstructor
@Getter
@Table(name = "concert",indexes = @Index(name="idx_api_concert_id", columnList = "api_concert_id"))
public class Concert {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "concert_id")
    private Long concertId;

    @ManyToOne(fetch = FetchType.LAZY)
    ConcertPlace concertPlace;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false,columnDefinition = "TEXT")
    private String content;

    @Column(name = "start_date",nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date",nullable = false)
    private LocalDate endDate;

    @Column(name = "ticket_time", nullable = true)
    private LocalDateTime ticketTime;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Column(name = "modified_date", nullable = false)
    private LocalDateTime modifiedDate;

    @Column(nullable = false)
    private int maxPrice;

    @Column(nullable = false)
    private int minPrice;

    @Column(name = "api_concert_id", nullable = false)
    private String apiConcertId;

    @Column(name = "poster_url",nullable = false,columnDefinition = "TEXT")
    private String posterUrl;

    private int viewCount;

    private int likeCount;



    public Concert(ConcertPlace concertPlace, String name, String content, LocalDate startDate, LocalDate endDate, LocalDateTime ticketTime, int maxPrice, int minPrice, String posterUrl,String apiConcertId) {
        this.concertPlace = concertPlace;
        this.name = name;
        this.content = content;
        this.ticketTime = ticketTime;
        this.startDate = startDate;
        this.endDate = endDate;
        this.createdDate = LocalDateTime.now();
        this.modifiedDate = LocalDateTime.now();
        this.maxPrice = maxPrice;
        this.minPrice = minPrice;
        this.posterUrl = posterUrl;
        this.likeCount = 0;
        this.viewCount = 0;
        this.apiConcertId = apiConcertId;
    }

    public Concert(Long concertId) {
        this.concertId = concertId;
    }

    public Concert updateByAPI(ConcertPlace concertPlace, String content,LocalDate startDate,LocalDate endDate, int maxPrice, int minPrice, String posterUrl) {
        this.concertPlace = concertPlace;
        this.content = content;
        this.startDate = startDate;
        this.endDate = endDate;
        this.modifiedDate = LocalDateTime.now();
        this.maxPrice = maxPrice;
        this.minPrice = minPrice;
        this.posterUrl = posterUrl;
        return this;
    }


    public Concert update(ConcertUpdateRequest concertUpdateRequest,ConcertPlace concertPlace) {
        this.name = concertUpdateRequest.getName();
        this.concertPlace = concertPlace;
        this.content = concertUpdateRequest.getDescription();
        this.maxPrice = concertUpdateRequest.getMaxPrice();
        this.minPrice = concertUpdateRequest.getMinPrice();
        this.posterUrl = concertUpdateRequest.getPosterUrl();
        this.startDate = concertUpdateRequest.getStartDate();
        this.endDate = concertUpdateRequest.getEndDate();
        this.modifiedDate = LocalDateTime.now();
        return this;
    }

    public Concert ticketTimeSet(LocalDateTime ticketTime){
        this.ticketTime = ticketTime;
        return this;
    }

}
