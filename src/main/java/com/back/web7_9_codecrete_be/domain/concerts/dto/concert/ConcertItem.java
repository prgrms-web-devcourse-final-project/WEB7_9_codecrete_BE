package com.back.web7_9_codecrete_be.domain.concerts.dto.concert;

import com.back.web7_9_codecrete_be.domain.concerts.entity.Concert;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
public class ConcertItem {

    @Schema(description = "콘서트 Id입니다.")
    private long id;

    @Schema(description = "콘서트 이름입니다.")
    private String name;

    @Schema(description = "콘서트 장소 이름입니다.")
    private String placeName;

    @Schema(description = "콘서트 예매 시작 날짜입니다.")
    private LocalDateTime ticketTime;

    @Schema(description = "콘서트 시작 날짜입니다.",format = "yyyy-MM-dd")
    private LocalDate startDate ;

    @Schema(description = "콘서트 종료 날짜입니다.",format = "yyyy-MM-dd")
    private LocalDate endDate ;

    @Schema(description = "콘서트 포스터URL입니다. 썸네일로 사용해주세요.")
    private String posterUrl;

    @Schema(description = "콘서트 티켓 최고가입니다.")
    private int maxPrice;

    @Schema(description = "콘서트 티켓 최저가입니다.")
    private int minPrice;

    @Schema(description = "콘서트 조회수입니다.")
    private int viewCount;

    @Schema(description = "콘서트 좋아요수입니다.")
    private int likeCount;

    public ConcertItem(Concert concert) {
        this.id = concert.getConcertId();
        this.name = concert.getName();
        this.placeName = concert.getConcertPlace().getPlaceName();
        this.ticketTime = concert.getTicketTime();
        this.startDate = concert.getStartDate();
        this.endDate =concert.getEndDate();
        this.posterUrl = concert.getPosterUrl();
        this.maxPrice = concert.getMaxPrice();
        this.minPrice = concert.getMinPrice();
        this.viewCount = concert.getViewCount();
        this.likeCount = concert.getLikeCount();
    }

    public ConcertItem(long id, String name, String placeName,LocalDateTime ticketTime, LocalDate startDate, LocalDate endDate, String posterUrl, int maxPrice, int minPrice, int viewCount, int likeCount) {
        this.id = id;
        this.name = name;
        this.placeName = placeName;
        this.ticketTime = ticketTime;
        this.startDate = startDate;
        this.endDate = endDate;
        this.posterUrl = posterUrl;
        this.maxPrice = maxPrice;
        this.minPrice = minPrice;
        this.viewCount = viewCount;
        this.likeCount = likeCount;
    }
}
