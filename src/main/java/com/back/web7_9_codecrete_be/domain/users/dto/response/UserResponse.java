package com.back.web7_9_codecrete_be.domain.users.dto.response;

import com.back.web7_9_codecrete_be.domain.users.entity.User;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class UserResponse {
    private Long id;
    private String email;
    private String nickname;
    private LocalDate birthdate;
    private String profileImageUrl;
    private String status;

    public static UserResponse from(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .birthdate(user.getBirth())
                .profileImageUrl(user.getProfileImage())
                .status(user.getStatus().name())
                .build();
    }
}
