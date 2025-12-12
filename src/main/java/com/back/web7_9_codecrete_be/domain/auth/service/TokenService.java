package com.back.web7_9_codecrete_be.domain.auth.service;

import com.back.web7_9_codecrete_be.domain.users.entity.User;
import com.back.web7_9_codecrete_be.global.error.code.AuthErrorCode;
import com.back.web7_9_codecrete_be.global.error.exception.BusinessException;
import com.back.web7_9_codecrete_be.global.rq.Rq;
import com.back.web7_9_codecrete_be.global.security.JwtProperties;
import com.back.web7_9_codecrete_be.global.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class TokenService {
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final Rq rq;

    // 로그인 시 실행 시 쿠키에 토큰 발급
    public void issueTokens(User user) {
        String access = jwtTokenProvider.generateAccessToken(user.getEmail());
        String refresh = jwtTokenProvider.generateRefreshToken(user.getEmail());

        rq.setCookie("ACCESS_TOKEN", access, (int) jwtProperties.getAccessTokenExpiration());
        rq.setCookie("REFRESH_TOKEN", refresh, (int) jwtProperties.getRefreshTokenExpiration());
    }

    // 로그아웃 시 실행 시 쿠키 삭제
    public void removeTokens(User user) {
        rq.removeCookie("ACCESS_TOKEN");
        rq.removeCookie("REFRESH_TOKEN");

    }

    public String reissueAccessToken() {

        // Refresh Token 쿠키 찾기
        String refresh = rq.getCookieValue("REFRESH_TOKEN");
        if (refresh == null) {
            throw new BusinessException(AuthErrorCode.TOKEN_MISSING);
        }

        // RefreshToken 검증
        jwtTokenProvider.validateToken(refresh);

        // RefreshToken에서 email(Subject) 추출
        String email = jwtTokenProvider.getEmailFromToken(refresh);

        // AccessToken 재발급
        String newAccess = jwtTokenProvider.generateAccessToken(email);

        // AccessToken 쿠키에 다시 저장
        rq.setCookie("ACCESS_TOKEN", newAccess, (int) jwtProperties.getAccessTokenExpiration());

        return newAccess;
    }
}
