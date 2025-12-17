package com.back.web7_9_codecrete_be.domain.auth.dto.kakao;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class KakaoUserInfo {
    private String socialId;
    private String email;
    private String nickname;
    private String profileImageUrl;
}
