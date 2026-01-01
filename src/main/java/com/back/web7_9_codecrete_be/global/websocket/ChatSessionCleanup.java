package com.back.web7_9_codecrete_be.global.websocket;

import java.util.Set;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ChatSessionCleanup {

	private final RedisTemplate<String, Object> redisTemplate;

	@PostConstruct
	public void cleanup() {
		String pattern = "chat:server:*";
		Set<String> keys = redisTemplate.keys(pattern);

		if (keys != null && !keys.isEmpty()) {
			redisTemplate.delete(keys);
		}
	}
}

