package com.back.web7_9_codecrete_be.global.security;

import com.back.web7_9_codecrete_be.domain.auth.service.TokenService;
import com.back.web7_9_codecrete_be.global.error.code.AuthErrorCode;
import com.back.web7_9_codecrete_be.global.error.exception.BusinessException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;


@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final TokenService tokenService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String accessToken  = resolveToken(request);

        // Access Token이 있는 경우 우선 검증 시도
        if (StringUtils.hasText(accessToken)) {
            try {
                if (jwtTokenProvider.validateToken(accessToken)) {
                    Authentication auth =
                            jwtTokenProvider.getAuthentication(accessToken);
                    SecurityContextHolder.getContext().setAuthentication(auth);
                    filterChain.doFilter(request, response);
                    return;
                }
            } catch (BusinessException e) {
                // Access Token 만료가 아닌 경우 재발급 안 함
                if (e.getErrorCode() != AuthErrorCode.TOKEN_EXPIRED) {
                    log.debug("Invalid access token: {}", e.getErrorCode());
                    filterChain.doFilter(request, response);
                    return;
                }
                // TOKEN_EXPIRED 인 경우만 아래 재발급 로직으로 내려감
            }
        }

        // Access Token이 없거나 / 만료된 경우 Refresh 기반 재발급 시도
        try {
            String newAccess = tokenService.reissueAccessToken();

            Authentication auth =
                    jwtTokenProvider.getAuthentication(newAccess);
            SecurityContextHolder.getContext().setAuthentication(auth);

        } catch (BusinessException ex) {
            // Refresh도 실패 → 익명 사용자 유지
            log.debug("Access Token 재발급 실패: {}", ex.getErrorCode());
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        if (request.getCookies() == null) return null;

        return Arrays.stream(request.getCookies())
                .filter(c -> "ACCESS_TOKEN".equals(c.getName()))
                .findFirst()
                .map(Cookie::getValue)
                .orElse(null);
    }
}
