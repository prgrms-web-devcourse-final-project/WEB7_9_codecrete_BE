package com.back.web7_9_codecrete_be.domain.auth.dto.response;

import com.back.web7_9_codecrete_be.domain.users.entity.Role;
import com.back.web7_9_codecrete_be.domain.users.entity.SocialType;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginResponse {
    private Long userId;
    private String nickname;
    private Role role;
    private SocialType socialType;
}
