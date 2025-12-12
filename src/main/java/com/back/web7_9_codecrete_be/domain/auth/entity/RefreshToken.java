package com.back.web7_9_codecrete_be.domain.auth.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RefreshToken {

    private Long userId;          // 사용자 ID
    private String refreshToken;  // 실제 토큰 값
    private long expiration;      // TTL (초 단위)
}
