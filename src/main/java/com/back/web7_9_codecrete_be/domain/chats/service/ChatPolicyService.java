package com.back.web7_9_codecrete_be.domain.chats.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;

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

		LocalDateTime policyStart = ticketTime.toLocalDate()
			.minusDays(3)
			.atStartOfDay();

		LocalDateTime policyEnd = ticketTime.toLocalDate()
			.plusDays(3)
			.atTime(LocalTime.MAX);

		boolean available =
			now.isAfter(policyStart) &&
				now.isBefore(policyEnd);

		// TTL 계산 후 캐싱
		Duration ttl = calculateTtl(now, policyStart, policyEnd);
		redisTemplate.opsForValue().set(key, available, ttl);

		return available;
	}

	private Duration calculateTtl(LocalDateTime now, LocalDateTime policyStart, LocalDateTime policyEnd) {

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

	public Duration calculateChatRemainingTtl(Long concertId) {

		Concert concert = concertRepository.getReferenceById(concertId);

		LocalDateTime ticketTime = concert.getTicketTime();
		if (ticketTime == null) {
			return Duration.ofMinutes(10);
		}

		LocalDateTime now = LocalDateTime.now();

		LocalDateTime policyEnd = ticketTime.toLocalDate()
			.plusDays(3)
			.atTime(LocalTime.MAX);

		if (now.isAfter(policyEnd)) {
			return Duration.ofMinutes(10);
		}

		return Duration.between(now, policyEnd);
	}


}
