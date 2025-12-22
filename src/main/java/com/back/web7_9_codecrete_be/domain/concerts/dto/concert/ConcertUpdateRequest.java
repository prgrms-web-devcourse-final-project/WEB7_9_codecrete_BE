package com.back.web7_9_codecrete_be.domain.concerts.dto.concert;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Value;

import java.time.LocalDate;

@Getter
public class ConcertUpdateRequest {
    @Schema(description = "수정할 대상이 될 공연 ID 입니다.")
    @NotNull(message = "공연 Id를 입력해 주세요.")
    private Long concertId;

    @Schema(description = "공연 이름입니다.")
    @NotBlank(message = "공연 이름을 입력하여 주십시오.")
    private String name;

    @Schema(description = "공연 설명입니다.")
    private String description;

    @Schema(description = "공연장 ID 입니다.")
    @NotNull(message = "공연장 Id를 입력해 주세요.")
    private Long placeId;

    @Schema(description = "공연 시작 날짜입니다.")
    @NotNull(message = "공연 시작 날짜 입력은 필수입니다.")
    private LocalDate StartDate;

    @Schema(description = "공연 종료 날짜입니다.")
    @NotNull(message = "공연 종료 날짜 입력은 필수입니다.")
    private LocalDate EndDate;

    @Schema(description = "공연 포스터 URL 입니다.")
    @NotBlank(message = "공연장 포스터 Url을 입력해주세요.")
    private String posterUrl;

    @Schema(description = "공연 티켓 최고가입니다.")
    @Positive(message = "표 가격은 양수여야 합니다.")
    private int maxPrice;

    @Schema(description = "공연 티켓 최저가입니다.")
    @Positive(message = "표 가격은 양수여야 합니다.")
    private int minPrice;
}
