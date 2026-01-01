package com.back.web7_9_codecrete_be.domain.chats.service;

import java.security.Principal;
import java.time.Duration;
import java.time.LocalDateTime;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import com.back.web7_9_codecrete_be.domain.chats.dto.request.ChatMessageRequest;
import com.back.web7_9_codecrete_be.domain.chats.dto.response.ChatMessageResponse;
import com.back.web7_9_codecrete_be.domain.chats.dto.response.ChatUserCache;
import com.back.web7_9_codecrete_be.domain.chats.repository.ChatStreamRepository;
import com.back.web7_9_codecrete_be.global.security.CustomUserDetail;
import com.back.web7_9_codecrete_be.global.websocket.ChatCountBroadcaster;
import com.back.web7_9_codecrete_be.global.websocket.ServerInstanceId;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMessageService {

	private final SimpMessagingTemplate messagingTemplate;
	private final ChatStreamRepository chatStreamRepository;
	private final ChatUserCacheService chatUserCacheService;

	private final RedisTemplate<String, Object> redisTemplate;
	private final ChatCountBroadcaster chatCountBroadcaster;

	/**
	 * 채팅 메시지 전송 + 세션 TTL 갱신
	 */
	public void handleSend(ChatMessageRequest request, Principal principal) {

		if (!(principal instanceof Authentication authentication)) {
			throw new IllegalStateException("Unauthenticated WebSocket access");
		}

		CustomUserDetail userDetail =
			(CustomUserDetail) authentication.getPrincipal();

		Long userId = userDetail.getUser().getId();

		extendUserSessionTtl(userId);

		sendMessage(request, userDetail.getUsername());
	}

	/**
	 * 실제 메시지 저장 + 브로드캐스트
	 */
	private void sendMessage(ChatMessageRequest request, String email) {

		ChatUserCache chatUser = chatUserCacheService.getChatUser(email);

		ChatMessageResponse response = new ChatMessageResponse(
			request.getConcertId(),
			chatUser.getUserId(),
			chatUser.getNickname(),
			request.getContent(),
			LocalDateTime.now()
		);

		log.info("[SEND MESSAGE] From User ID: {}, Content: {}",
			chatUser.getUserId(), request.getContent());

		// Redis Stream 저장
		chatStreamRepository.save(response);

		// WebSocket 브로드캐스트
		messagingTemplate.convertAndSend(
			"/topic/chat/" + request.getConcertId(),
			response
		);
	}

	/**
	 * 유저 세션 TTL 연장
	 */
	private void extendUserSessionTtl(Long userId) {

		String sessionSetKey =
			"chat:server:" + ServerInstanceId.ID + ":user:" + userId + ":sessionIds";

		redisTemplate.expire(sessionSetKey, Duration.ofHours(2));

		log.debug("[CHAT TTL] session TTL extended: userId={}, key={}", userId, sessionSetKey);
	}


	/**
	 * 채팅방 접속자 수 브로드캐스트
	 */
	public void broadcastUserCount(Long concertId) {

		String key = "chat:concert:" + concertId + ":users";
		Long count = redisTemplate.opsForSet().size(key);
		long userCount = (count != null) ? count : 0;

		chatCountBroadcaster.broadcast(concertId, userCount);

		log.info("[CHAT STATUS] 인원수 응답 완료: concertId={}, count={}", concertId, userCount);
	}
}
