package com.back.web7_9_codecrete_be.domain.concerts.dto.concert;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;

import java.time.LocalDate;

@Getter
public class ConcertUpdateRequest {
    @Schema(description = "수정할 대상이 될 공연 ID 입니다.")
    @NotEmpty
    private Long concertId;

    @Schema(description = "공연 이름입니다.")
    @NotEmpty
    private String name;

    @Schema(description = "공연 설명입니다.")
    @NotEmpty
    private String description;

    @Schema(description = "공연장 ID 입니다.")
    @NotEmpty
    private Long placeId;

    @Schema(description = "공연 시작 날짜입니다.")
    @NotEmpty
    private LocalDate StartDate;

    @Schema(description = "공연 종료 날짜입니다.")
    @NotEmpty
    private LocalDate EndDate;

    @Schema(description = "공연 포스터 URL 입니다.")
    @NotEmpty
    private String posterUrl;

    @Schema(description = "공연 티켓 최고가입니다.")
    @NotEmpty
    private int maxPrice;

    @Schema(description = "공연 티켓 최저가입니다.")
    @NotEmpty
    private int minPrice;
}
