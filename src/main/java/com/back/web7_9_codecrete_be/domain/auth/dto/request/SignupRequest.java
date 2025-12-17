package com.back.web7_9_codecrete_be.domain.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;

@Getter
@Schema(description = "회원가입 요청 DTO")
public class SignupRequest {

    @NotBlank(message = "이메일은 필수입니다.")
    @Pattern(
            regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$",
            message = "이메일 형식이 올바르지 않습니다."
    )
    @Schema(description = "사용자 이메일", example = "test@example.com")
    private String email;

    @NotBlank(message = "닉네임은 필수입니다.")
    @Schema(description = "닉네임", example = "codeMaster")
    private String nickname;

    @NotBlank(message = "비밀번호는 필수입니다.")
    @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[!@#$%^&*]).{8,}$",
            message = "비밀번호는 영문, 숫자, 특수문자를 포함한 8자 이상이어야 합니다."
    )
    @Schema(description = "비밀번호", example = "1234abcd!")
    private String password;

    @NotBlank(message = "생년월일은 필수입니다.")
    @Pattern(
            regexp = "\\d{4}-\\d{2}-\\d{2}",
            message = "생년월일은 yyyy-MM-dd 형식이어야 합니다."
    )
    @Schema(description = "생년월일", example = "2000-08-25")
    private String birth;

    @Schema(description = "프로필 이미지 URL", example = "https://image.com/profile.png")
    private String profileImage;
}
