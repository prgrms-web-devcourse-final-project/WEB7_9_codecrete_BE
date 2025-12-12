package com.back.web7_9_codecrete_be.domain.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "회원가입 요청 DTO")
public class SignupRequest {
    @Schema(description = "사용자 이메일", example = "test@example.com")
    private String email;

    @Schema(description = "닉네임", example = "codeMaster")
    private String nickname;

    @Schema(description = "비밀번호", example = "1234abcd!")
    private String password;

    @Schema(description = "생년월일", example = "2000-08-25")
    private String birth;

    @Schema(description = "프로필 이미지 URL", example = "https://image.com/profile.png")
    private String profileImage;
}
