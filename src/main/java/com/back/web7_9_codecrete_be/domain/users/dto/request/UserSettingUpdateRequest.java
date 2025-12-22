package com.back.web7_9_codecrete_be.domain.users.dto.request;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
public class UserSettingUpdateRequest{
        @Schema(description = "이메일 알림 설정", example = "true")
        Boolean emailNotifications;

        @Schema(description = "다크 모드 설정", example = "false")
        Boolean darkMode;
}
