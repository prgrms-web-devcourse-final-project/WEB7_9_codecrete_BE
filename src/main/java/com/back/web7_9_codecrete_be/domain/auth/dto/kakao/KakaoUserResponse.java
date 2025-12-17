package com.back.web7_9_codecrete_be.domain.auth.dto.kakao;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class KakaoUserResponse {

    private Long id;

    @JsonProperty("kakao_account")
    private KakaoAccount kakaoAccount;

    public KakaoUserInfo toUserInfo() {
        return new KakaoUserInfo(
                String.valueOf(id),
                kakaoAccount.getEmail(),
                kakaoAccount.getProfile().getNickname(),
                kakaoAccount.getProfile().getProfileImageUrl()
        );
    }

    @Getter
    public static class KakaoAccount {
        private String email;
        private Profile profile;
    }

    @Getter
    public static class Profile {
        private String nickname;

        @JsonProperty("profile_image_url")
        private String profileImageUrl;
    }
}
