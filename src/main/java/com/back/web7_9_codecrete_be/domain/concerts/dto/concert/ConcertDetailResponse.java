package com.back.web7_9_codecrete_be.domain.concerts.dto.concert;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class ConcertDetailResponse {

    @Schema(description = "콘서트 Id입니다.")
    private Long concertId;

    @Schema(description = "콘서트 이름입니다.")
    private String name;

    @Schema(description = "콘서트에 대한 설명입니다.")
    private String description;

    @Schema(description = "콘서트 장소 이름입니다.")
    private String placeName;

    @Schema(description = "콘서트 장 주소입니다.")
    private String placeAddress;

    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @Schema(description = "콘서트 예매 시작 날짜입니다.")
    private LocalDateTime ticketTime;

    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @Schema(description = "콘서트 예매 종료 날짜입니다.")
    private LocalDateTime ticketEndTime;

    @JsonSerialize(using = LocalDateSerializer.class)
    @JsonDeserialize(using = LocalDateDeserializer.class)
    @Schema(description = "콘서트 시작 날짜입니다.",format = "yyyy-MM-dd")
    private LocalDate startDate;

    @JsonSerialize(using = LocalDateSerializer.class)
    @JsonDeserialize(using = LocalDateDeserializer.class)
    @Schema(description = "콘서트 종료 날짜입니다.",format = "yyyy-MM-dd")
    private LocalDate endDate;

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

    @Schema(description = "콘서트 이미지 목록입니다.")
    private List<String> concertImageUrls;

    @Schema(description = "콘서트 참여 아티스트 목록입니다.")
    private List<Long> concertArtists;

}
