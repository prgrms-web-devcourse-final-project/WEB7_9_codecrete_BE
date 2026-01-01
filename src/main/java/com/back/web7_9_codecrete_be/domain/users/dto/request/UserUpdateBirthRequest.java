package com.back.web7_9_codecrete_be.domain.users.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdateBirthRequest {

    @NotBlank(message = "생년월일은 필수입니다.")
    @Pattern(
            regexp = "\\d{4}-\\d{2}-\\d{2}",
            message = "생년월일은 yyyy-MM-dd 형식이어야 합니다."
    )
    private String birth;
}
