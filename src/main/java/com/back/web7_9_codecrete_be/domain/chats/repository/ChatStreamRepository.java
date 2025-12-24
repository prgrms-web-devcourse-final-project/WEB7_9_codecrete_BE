package com.back.web7_9_codecrete_be.domain.chats.repository;

import java.time.Duration;

import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import com.back.web7_9_codecrete_be.domain.chats.dto.ChatMessageResponse;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ChatStreamRepository {

	private final RedisTemplate<String, Object> redisTemplate;

	private String streamKey(Long concertId) {
		return "chat:stream:" + concertId;
	}

	public void save(ChatMessageResponse message) {
		String key = streamKey(message.getConcertId());

		redisTemplate.opsForStream().add(
			StreamRecords.newRecord()
				.in(key)
				.ofObject(message)
		);
	}

	public boolean hasTtl(Long concertId) {
		Long ttl = redisTemplate.getExpire(streamKey(concertId));
		return ttl != null && ttl >= 0;
	}

	public void setTtl(Long concertId, Duration ttl) {
		redisTemplate.expire(streamKey(concertId), ttl);
	}


}
