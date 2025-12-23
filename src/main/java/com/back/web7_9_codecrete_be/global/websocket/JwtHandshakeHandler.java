package com.back.web7_9_codecrete_be.global.websocket;

import java.security.Principal;
import java.util.Map;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import com.back.web7_9_codecrete_be.global.error.code.AuthErrorCode;
import com.back.web7_9_codecrete_be.global.error.exception.BusinessException;
import com.back.web7_9_codecrete_be.global.security.JwtTokenProvider;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

/**
 * WebSocket Handshake 단계에서 쿠키에 포함된 JWT를 추출하여
 * 사용자 Authentication(Principal)을 설정하는 HandshakeHandler.
 *
 * HTTP 필터 체인이 적용되지 않는 WebSocket 연결 단계에서
 * 별도의 인증 처리를 담당
 */
@Component
@RequiredArgsConstructor
public class JwtHandshakeHandler extends DefaultHandshakeHandler {

	private final JwtTokenProvider jwtTokenProvider;

	@Override
	protected Principal determineUser(
		ServerHttpRequest request,
		WebSocketHandler wsHandler,
		Map<String, Object> attributes) {

		if (!(request instanceof ServletServerHttpRequest servletRequest)) {
			throw new BusinessException(AuthErrorCode.UNAUTHORIZED_USER);
		}

		HttpServletRequest httpRequest = servletRequest.getServletRequest();

		if (httpRequest.getCookies() == null) {
			throw new BusinessException(AuthErrorCode.UNAUTHORIZED_USER);
		}

		for (Cookie cookie : httpRequest.getCookies()) {
			if ("ACCESS_TOKEN".equals(cookie.getName())) {
				String token = cookie.getValue();

				return jwtTokenProvider.getAuthentication(token);
			}
		}

		throw new BusinessException(AuthErrorCode.UNAUTHORIZED_USER);
	}
}

