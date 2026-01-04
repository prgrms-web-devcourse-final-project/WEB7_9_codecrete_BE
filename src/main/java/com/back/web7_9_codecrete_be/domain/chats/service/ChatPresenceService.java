package com.back.web7_9_codecrete_be.domain.chats.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.back.web7_9_codecrete_be.global.websocket.ChatStompBroadcaster;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChatPresenceService {

	private final RedisTemplate<String, Object> redisTemplate;
	private final ChatParticipantService chatParticipantService;
	private final ChatStompBroadcaster chatStompBroadcaster;

	private String concertUserKey(Long concertId) {
		return "chat:concert:" + concertId + ":users";
	}

	public void broadcast(Long concertId) {

		Long count =
			redisTemplate.opsForSet().size(concertUserKey(concertId));

		chatStompBroadcaster.broadcast(
			"/topic/chat/" + concertId + "/count",
			count != null ? count : 0
		);

		chatStompBroadcaster.broadcast(
			"/topic/chat/" + concertId + "/users",
			chatParticipantService.getParticipants(concertId)
		);
	}
}
