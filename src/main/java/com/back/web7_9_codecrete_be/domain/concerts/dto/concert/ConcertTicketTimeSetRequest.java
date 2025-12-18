package com.back.web7_9_codecrete_be.domain.concerts.dto.concert;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ConcertTicketTimeSetRequest {
    @Schema(description = "공연 ID 입니다.")
    @NotEmpty
    private Long  concertId;

    @Schema(description = "티켓팅 시간입니다.")
    @NotEmpty
    private LocalDateTime ticketTime;
}
