package com.back.web7_9_codecrete_be.domain.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "로그인 요청 DTO")
public class LoginRequest {
    @Schema(description = "사용자 이메일", example = "test@example.com")
    private String email;

    @Schema(description = "비밀번호", example = "1234abcd!")
    private String password;
}
