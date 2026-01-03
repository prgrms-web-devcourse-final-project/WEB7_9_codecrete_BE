package com.back.web7_9_codecrete_be.domain.chats.service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.back.web7_9_codecrete_be.domain.chats.dto.response.ChatParticipantResponse;
import com.back.web7_9_codecrete_be.domain.users.entity.User;
import com.back.web7_9_codecrete_be.domain.users.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChatParticipantService {

	private final RedisTemplate<String, Object> redisTemplate;
	private final UserRepository userRepository;

	private String concertUserKey(Long concertId) {
		return "chat:concert:" + concertId + ":users";
	}

	public List<ChatParticipantResponse> getParticipants(Long concertId) {

		Set<Object> userIds =
			redisTemplate.opsForSet().members(concertUserKey(concertId));

		if (userIds == null || userIds.isEmpty()) {
			return List.of();
		}

		List<Long> ids = userIds.stream()
			.map(id -> Long.valueOf(id.toString()))
			.toList();

		List<User> users = userRepository.findAllById(ids);

		return users.stream()
			.map(user -> new ChatParticipantResponse(
				user.getId(),
				user.getNickname(),
				user.getProfileImage()
			))
			.collect(Collectors.toList());
	}
}
