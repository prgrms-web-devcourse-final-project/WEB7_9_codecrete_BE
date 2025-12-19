package com.back.web7_9_codecrete_be.domain.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TokenResponse {
    @Schema(description = "액세스 토큰")
    private String accessToken;
}
