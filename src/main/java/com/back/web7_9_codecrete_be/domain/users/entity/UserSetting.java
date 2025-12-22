package com.back.web7_9_codecrete_be.domain.users.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "user_settings")
public class UserSetting {
    @Id
    private Long userId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @MapsId
    private User user;

    @Column(name = "email_notifications", nullable = false)
    private boolean emailNotifications;

    @Column(name = "dark_mode", nullable = false)
    private boolean darkMode;

    public UserSetting(User user) {
        this.user = user;
        this.emailNotifications = true; // 기본값 설정
        this.darkMode = false; // 기본값 설정
    }

    public void changeEmailNotifications(boolean emailNotifications) {
        this.emailNotifications = emailNotifications;
    }

    public void changeDarkMode(boolean darkMode) {
        this.darkMode = darkMode;
    }

}
