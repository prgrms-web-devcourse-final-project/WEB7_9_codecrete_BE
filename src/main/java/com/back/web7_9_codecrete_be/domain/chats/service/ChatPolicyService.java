package com.back.web7_9_codecrete_be.domain.chats.service;

import java.time.Duration;
import java.time.LocalDateTime;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.back.web7_9_codecrete_be.domain.concerts.entity.Concert;
import com.back.web7_9_codecrete_be.domain.concerts.repository.ConcertRepository;
import com.back.web7_9_codecrete_be.global.error.code.ConcertErrorCode;
import com.back.web7_9_codecrete_be.global.error.exception.BusinessException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChatPolicyService {

	private final RedisTemplate<String, Object> redisTemplate;
	private final ConcertRepository concertRepository;

	private static final String CHAT_POLICY_KEY_PREFIX = "chat:policy:concert:";

	public boolean isChatAvailable(Long concertId) {

		String key = CHAT_POLICY_KEY_PREFIX + concertId;

		// redis 캐시 조회
		Boolean cached = (Boolean)redisTemplate.opsForValue().get(key);
		if (cached != null) {
			return cached;
		}

		// 캐시 미스 -> Concert db 조회
		Concert concert = concertRepository.findById(concertId)
			.orElseThrow(() -> new BusinessException(ConcertErrorCode.CONCERT_NOT_FOUND));

		LocalDateTime ticketTime = concert.getTicketTime();
		if (ticketTime == null) {
			return false;
		}

		// 정책 기간 계산
		LocalDateTime now = LocalDateTime.now();
		boolean available =
			now.isAfter(ticketTime.minusDays(3)) &&
				now.isBefore(ticketTime.plusDays(3));

		// TTL 계산 후 캐싱
		Duration ttl = calculateTtl(now, ticketTime);
		redisTemplate.opsForValue().set(key, available, ttl);

		return available;
	}

	private Duration calculateTtl(LocalDateTime now, LocalDateTime ticketTime) {

		LocalDateTime policyStart = ticketTime.minusDays(3);
		LocalDateTime policyEnd   = ticketTime.plusDays(3);

		// 아직 정책 시작 전
		if (now.isBefore(policyStart)) {
			return Duration.between(now, policyStart);
		}

		// 정책 기간 중
		if (now.isBefore(policyEnd)) {
			return Duration.between(now, policyEnd);
		}

		// 정책 기간 종료 후
		return Duration.ofMinutes(10);
	}
}
