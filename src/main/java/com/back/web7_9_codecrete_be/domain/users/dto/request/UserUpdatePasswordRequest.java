package com.back.web7_9_codecrete_be.domain.users.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;

@Getter
public class UserUpdatePasswordRequest {

    @NotBlank(message = "현재 비밀번호는 필수입니다.")
    @Schema(description = "현재 비밀번호", example = "oldPassword1!")
    private String currentPassword;

    @NotBlank(message = "비밀번호는 필수입니다.")
    @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[!@#$%^&*]).{8,}$",
            message = "비밀번호는 영문, 숫자, 특수문자를 포함한 8자 이상이어야 합니다."
    )
    @Schema(description = "비밀번호", example = "1234abcd!")
    private String newPassword;
}
