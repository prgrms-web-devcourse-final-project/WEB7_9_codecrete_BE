package com.back.web7_9_codecrete_be.global.websocket;

import java.time.Duration;
import java.util.Map;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.back.web7_9_codecrete_be.domain.chats.service.ChatPresenceService;
import com.back.web7_9_codecrete_be.global.security.CustomUserDetail;
import com.back.web7_9_codecrete_be.global.security.JwtTokenProvider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatStompHandler implements ChannelInterceptor {

	private final RedisTemplate<String, Object> redisTemplate;
	private final JwtTokenProvider jwtTokenProvider;
	private final ChatPresenceService chatPresenceService;

	private String getConcertUserKey(Long concertId) {
		return "chat:concert:" + concertId + ":users";
	}
	private String getUserSessionsSetKey(Long userId) {
		return "chat:server:" + ServerInstanceId.ID + ":user:" + userId + ":sessionIds";
	}

	@Override
	public Message<?> preSend(Message<?> message, MessageChannel channel) {
		StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

		if (StompCommand.CONNECT.equals(accessor.getCommand())) {
			handleConnect(accessor);
		} else if (StompCommand.DISCONNECT.equals(accessor.getCommand())) {
			handleDisconnect(accessor);
		}

		return message;
	}

	private void handleConnect(StompHeaderAccessor accessor) {

		String token = extractTokenFromCookie(accessor);

		if (token == null || !jwtTokenProvider.validateToken(token)) {
			log.error("[CHAT CONNECT] 인증 실패: 토큰이 없거나 유효하지 않음");
			return;
		}

		Authentication auth = jwtTokenProvider.getAuthentication(token);
		CustomUserDetail userDetail = (CustomUserDetail) auth.getPrincipal();
		Long userId = userDetail.getUser().getId();

		String concertIdHeader = accessor.getFirstNativeHeader("concertId");
		if (concertIdHeader == null) {
			log.error("[CHAT CONNECT] concertId 헤더가 없습니다.");
			return;
		}
		Long concertId = Long.valueOf(concertIdHeader);

		String sessionId = accessor.getSessionId();
		String sessionSetKey = getUserSessionsSetKey(userId);

		// 중복 CONNECT 차단
		Boolean added = redisTemplate.opsForSet().add(sessionSetKey, sessionId) == 1;
		if (!added) {
			log.warn("[CHAT CONNECT] 중복 CONNECT 무시 sessionId={}", sessionId);
			return;
		}

		// 세션 TTL (비정상 종료 대비)
		redisTemplate.expire(sessionSetKey, Duration.ofHours(2));

		// 세션 속성에 저장 (Disconnect 시 Redis 키를 찾기 위해 필요)
		// accessor.getSessionAttributes()는 웹소켓 세션 동안 유지되는 Map
		if (accessor.getSessionAttributes() != null) {
			accessor.getSessionAttributes().put("concertId", concertId);
			accessor.getSessionAttributes().put("userId", userId);
		}

		// 공연별 접속자 유저 ID 목록(Set)에 추가 (Set -> 동일 유저는 한 번만 기록됨)
		redisTemplate.opsForSet().add(getConcertUserKey(concertId), userId);

		chatPresenceService.broadcast(concertId);

		log.info("[CHAT CONNECT] 성공: concertId={}, userId={}, email={}",
			concertId, userId, userDetail.getUsername());
	}

	private void handleDisconnect(StompHeaderAccessor accessor) {
		Map<String, Object> session = accessor.getSessionAttributes();
		if (session == null || !session.containsKey("userId")) return;

		Long concertId = (Long) session.get("concertId");
		Long userId = (Long) session.get("userId");
		String sessionId = accessor.getSessionId();
		String sessionSetKey = getUserSessionsSetKey(userId);

		redisTemplate.opsForSet().remove(sessionSetKey, sessionId);

		// 활성화된 세션 개수
		Long remainingSessions = redisTemplate.opsForSet().size(sessionSetKey);

		// 모든 탭이 닫혔을 때만 실제 접속자 목록에서 삭제
		if (remainingSessions == null || remainingSessions <= 0) {
			redisTemplate.opsForSet().remove(getConcertUserKey(concertId), userId);
			redisTemplate.delete(sessionSetKey);
			log.info("[CHAT EXIT] 모든 탭 종료 - concertId={}, userId={}", concertId, userId);
		} else {
			log.info("[CHAT DISCONNECT] 탭 하나 종료 - userId={}, 남은 세션={}", userId, remainingSessions);
		}

		chatPresenceService.broadcast(concertId);
	}

	private String extractTokenFromCookie(StompHeaderAccessor accessor) {
		Map<String, Object> attributes = accessor.getSessionAttributes();
		if (attributes == null) return null;

		return (String) attributes.get("ACCESS_TOKEN");
	}
}
