package com.back.web7_9_codecrete_be.domain.auth.dto.google;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class GoogleUserInfo {
    private String socialId;
    private String email;
    private String nickname;
    private String profileImageUrl;
}
