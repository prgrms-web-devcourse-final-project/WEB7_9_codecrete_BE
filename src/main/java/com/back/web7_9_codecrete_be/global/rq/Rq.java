package com.back.web7_9_codecrete_be.global.rq;

import com.back.web7_9_codecrete_be.domain.users.entity.User;
import com.back.web7_9_codecrete_be.global.error.code.AuthErrorCode;
import com.back.web7_9_codecrete_be.global.error.exception.BusinessException;
import com.back.web7_9_codecrete_be.global.security.CustomUserDetail;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class Rq {

    private final HttpServletRequest request;
    private final HttpServletResponse response;

    public Rq(HttpServletRequest request, HttpServletResponse response) {
        this.request = request;
        this.response = response;
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
    public void setCookie(String name, String value, int maxAge) {
        String safeValue = value != null ? value : "";

        Cookie cookie = new Cookie(name, safeValue);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(maxAge);
        cookie.setSecure(true); // https 환경 권장 옵션

        response.addCookie(cookie);
    }

    // 쿠키 제거
    public void removeCookie(String name) {
        Cookie cookie = new Cookie(name, null);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);

        response.addCookie(cookie);
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