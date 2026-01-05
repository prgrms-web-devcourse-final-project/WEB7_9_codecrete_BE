package com.back.web7_9_codecrete_be.domain.chats.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.back.web7_9_codecrete_be.global.redis.ChatPubSubChannels;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChatPresenceService {

	private final RedisTemplate<String, Object> redisTemplate;

	public void broadcast(Long concertId) {
		redisTemplate.convertAndSend(
			ChatPubSubChannels.CHAT_PRESENCE,
			concertId
		);
	}
}
