package com.back.web7_9_codecrete_be.global.security;

import com.back.web7_9_codecrete_be.domain.auth.service.TokenService;
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

        String accessToken = resolveToken(request);

        log.info("[JwtFilter] URI = {}", request.getRequestURI());
        log.info("[JwtFilter] ACCESS_TOKEN = {}", accessToken);

        if (StringUtils.hasText(accessToken)) {
            try {
                log.info("[JwtFilter] validating token");

                if (jwtTokenProvider.validateToken(accessToken)) {
                    log.info("[JwtFilter] token valid");

                    Authentication auth =
                            jwtTokenProvider.getAuthentication(accessToken);

                    log.info("[JwtFilter] auth = {}", auth);

                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (BusinessException e) {
                log.info("[JwtFilter] token invalid: {}", e.getErrorCode());
                SecurityContextHolder.clearContext();
            }
        }

        log.info("[JwtFilter] SecurityContext = {}",
                SecurityContextHolder.getContext().getAuthentication());

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
