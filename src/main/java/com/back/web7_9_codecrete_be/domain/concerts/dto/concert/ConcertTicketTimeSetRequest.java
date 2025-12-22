package com.back.web7_9_codecrete_be.domain.concerts.dto.concert;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ConcertTicketTimeSetRequest {
    @Schema(description = "공연 ID 입니다.")
    @NotNull(message = "예매 시작 시간 입력시 해당 공연의 ID 값 입력은 필수입니다.")
    private Long  concertId;

    @Schema(description = "티켓팅 시간입니다.")
    @NotNull(message = "예매 시작 시간 설정시 시간 입력은 필수입니다.")
    private LocalDateTime ticketTime;
}
