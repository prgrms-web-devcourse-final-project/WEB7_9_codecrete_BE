package com.back.web7_9_codecrete_be.domain.users.dto.response;


import com.back.web7_9_codecrete_be.domain.users.entity.User;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserPublicResponse {
    private Long id;
    private String nickname;
    private String profileImageUrl;

    public static UserPublicResponse from(User user) {
        return UserPublicResponse.builder()
                .id(user.getId())
                .nickname(user.getNickname())
                .profileImageUrl(user.getProfileImage())
                .build();
    }
}
