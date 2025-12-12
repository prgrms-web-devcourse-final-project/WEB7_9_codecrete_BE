package com.back.web7_9_codecrete_be.global.security;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Getter
@Component
public class JwtProperties {

    @Value("${jwt.secret}")
    private String secretKey;

    // 초 단위 (예: 3600 = 1시간)
    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration;

    // 초 단위 (예: 1209600 = 14일)
    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    @Value("${jwt.access-token-header:Authorization}")
    private String accessHeader;

    @Value("${jwt.refresh-token-header:Refresh-Token}")
    private String refreshHeader;
}
