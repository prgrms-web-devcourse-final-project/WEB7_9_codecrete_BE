package com.back.web7_9_codecrete_be.domain.users.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdateBirthRequest {

    @NotNull(message = "생일은 필수 입력값입니다.")
    private LocalDate birth;
}
