package com.back.web7_9_codecrete_be.domain.chats.service;

import java.time.Duration;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.back.web7_9_codecrete_be.domain.chats.dto.response.ChatUserCache;
import com.back.web7_9_codecrete_be.domain.users.entity.User;
import com.back.web7_9_codecrete_be.domain.users.repository.UserRepository;
import com.back.web7_9_codecrete_be.global.error.code.AuthErrorCode;
import com.back.web7_9_codecrete_be.global.error.exception.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChatUserCacheService {

	private static final Duration ttl = Duration.ofMinutes(120);

	private final RedisTemplate<String, Object> redisTemplate;
	private final UserRepository userRepository;
	private final ObjectMapper objectMapper;

	private String cacheKey(String email) {
		return "chat:user:" + email;
	}

	public ChatUserCache getChatUser(String email) {
		String key = cacheKey(email);

		Object cached = redisTemplate.opsForValue().get(key);

		if (cached != null) {
			return objectMapper.convertValue(cached, ChatUserCache.class);
		}

		User user = userRepository.findByEmail(email)
			.orElseThrow(() -> new BusinessException(AuthErrorCode.USER_NOT_FOUND));

		ChatUserCache cache = new ChatUserCache(
			user.getId(),
			user.getNickname(),
			user.getProfileImage()
		);

		redisTemplate.opsForValue().set(key, cache, ttl);

		return cache;
	}

	public void removeChatUserCache(String email) {
		redisTemplate.delete(cacheKey(email));
	}
}
