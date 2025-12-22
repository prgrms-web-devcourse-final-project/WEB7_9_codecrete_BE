package com.back.web7_9_codecrete_be.domain.users.dto.response;

import com.back.web7_9_codecrete_be.domain.users.entity.UserSetting;
import lombok.Getter;

@Getter
public class UserSettingResponse {

    private final boolean emailNotifications;
    private final boolean darkMode;

    public UserSettingResponse(boolean emailNotifications, boolean darkMode) {
        this.emailNotifications = emailNotifications;
        this.darkMode = darkMode;
    }

    public static UserSettingResponse from(UserSetting setting) {
        return new UserSettingResponse(
                setting.isEmailNotifications(),
                setting.isDarkMode()
        );
    }
}
