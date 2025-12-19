package com.back.web7_9_codecrete_be.global.rq;

import com.back.web7_9_codecrete_be.domain.users.entity.User;
import com.back.web7_9_codecrete_be.global.error.code.AuthErrorCode;
import com.back.web7_9_codecrete_be.global.error.exception.BusinessException;
import com.back.web7_9_codecrete_be.global.security.CustomUserDetail;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class Rq {

    private final HttpServletRequest request;
    private final HttpServletResponse response;
    private final boolean isProd;

    public Rq(HttpServletRequest request,
              HttpServletResponse response,
              @Value("${spring.profiles.active:local}") String activeProfile)
    {
        this.request = request;
        this.response = response;
        this.isProd = activeProfile.equals("prod");
    }

    // 현재 인증된 사용자 정보 가져오기
    public User getUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getPrincipal() == null) {
            throw new BusinessException(AuthErrorCode.UNAUTHORIZED_USER);
        }

        if (!(authentication.getPrincipal() instanceof CustomUserDetail)) {
            throw new BusinessException(AuthErrorCode.UNAUTHORIZED_USER);
        }

        CustomUserDetail userDetails = (CustomUserDetail) authentication.getPrincipal();
        return userDetails.getUser();
    }

    // 쿠키 설정
    public void setCookie(String name, String value, long maxAge) {
        String safeValue = value != null ? value : "";

        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, safeValue)
                .path("/")
                .httpOnly(true)
                .maxAge(maxAge);

        if (isProd) {
            builder
                    .secure(true)
                    .sameSite("None")
                    .domain(".naeconcertbutakhae.shop");
        } else {
            builder
                    .secure(false)
                    .sameSite("Lax");
        }

        response.addHeader("Set-Cookie", builder.build().toString());
    }

    // 쿠키 제거
    public void removeCookie(String name) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, "")
                .path("/")
                .httpOnly(true)
                .maxAge(0);

        if (isProd) {
            builder
                    .secure(true)
                    .sameSite("None")
                    .domain(".naeconcertbutakhae.shop");
        } else {
            builder
                    .secure(false)
                    .sameSite("Lax");
        }

        response.addHeader("Set-Cookie", builder.build().toString());
    }

    public String getCookieValue(String name) {
        if (request.getCookies() == null) return null;

        for (Cookie cookie : request.getCookies()) {
            if (cookie.getName().equals(name)) {
                return cookie.getValue();
            }
        }

        return null;
    }
}