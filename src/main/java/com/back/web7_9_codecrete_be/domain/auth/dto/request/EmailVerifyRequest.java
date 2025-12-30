package com.back.web7_9_codecrete_be.domain.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;

@Getter
public class EmailVerifyRequest {

    @NotBlank(message = "이메일은 필수입니다.")
    @Pattern(
            regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$",
            message = "이메일 형식이 올바르지 않습니다."
    )
    @Schema(description = "사용자 이메일", example = "test@example.com")
    private String email;

    @NotBlank(message = "인증 코드는 필수입니다.")
    @Pattern(
            regexp = "^[A-Z0-9]{6}$",
            message = "인증 코드는 영문 대문자와 숫자를 포함한 6자리여야 합니다."
    )
    private String code;
}
