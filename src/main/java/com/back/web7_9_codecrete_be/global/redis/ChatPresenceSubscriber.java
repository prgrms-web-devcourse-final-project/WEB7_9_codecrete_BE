package com.back.web7_9_codecrete_be.global.redis;

import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import com.back.web7_9_codecrete_be.domain.chats.service.ChatParticipantService;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatPresenceSubscriber implements MessageListener {

	private final RedisTemplate<String, Object> redisTemplate;
	private final ChatParticipantService chatParticipantService;
	private final SimpMessagingTemplate messagingTemplate;
	private final ObjectMapper objectMapper;

	private String concertUserKey(Long concertId) {
		return "chat:concert:" + concertId + ":users";
	}

	@Override
	public void onMessage(
		Message message,
		byte[] pattern
	) {
		try {
			Long concertId =
				objectMapper.readValue(message.getBody(), Long.class);

			Long count =
				redisTemplate.opsForSet().size(concertUserKey(concertId));

			messagingTemplate.convertAndSend(
				"/topic/chat/" + concertId + "/count",
				count != null ? count : 0
			);

			messagingTemplate.convertAndSend(
				"/topic/chat/" + concertId + "/users",
				chatParticipantService.getParticipants(concertId)
			);

		} catch (Exception e) {
			log.error("[Redis Pub/Sub] Presence 처리 실패", e);
		}
	}
}
