package com.back.web7_9_codecrete_be.domain.serverTime.service;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.back.web7_9_codecrete_be.domain.serverTime.dto.ServerTimeMeasureResult;
import com.back.web7_9_codecrete_be.domain.serverTime.dto.ServerTimeResponse;
import com.back.web7_9_codecrete_be.domain.serverTime.entity.TicketProvider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class TicketServerTimeService {

	private final RestTemplate restTemplate;
	private final RedisTemplate<String, Object> redisTemplate;

	private static final int MEASURE_COUNT = 5;
	private static final int MIN_VALID_SAMPLE = 3;
	private static final ZoneId KST = ZoneId.of("Asia/Seoul");

	private static final Duration OFFSET_TTL = Duration.ofSeconds(60);
	private static final String REDIS_KEY_PREFIX = "serverTime:offset:";

	/**
	 * 외부 티켓 서버 시간 조회
	 */
	public ServerTimeResponse fetchServerTime(TicketProvider provider) {

		String redisKey = REDIS_KEY_PREFIX + provider.name();

		Number cached = (Number) redisTemplate.opsForValue().get(redisKey);
		Long offsetMillis = cached != null ? cached.longValue() : null;

		// 캐시 HIT
		if (offsetMillis != null) {

			log.info("[SERVER TIME][CACHE HIT] provider={}, offset={}ms",
				provider.name(), offsetMillis);

			return new ServerTimeResponse(
				provider.name(),
				offsetMillis
			);
		}


		// 캐시 MISS
		long medianOffset = measureMedianOffset(provider);

		redisTemplate.opsForValue()
			.set(redisKey, medianOffset, OFFSET_TTL);

		log.info("[SERVER TIME][CACHE MISS] provider={}, offset={}ms",
			provider.name(), medianOffset);

		return new ServerTimeResponse(
			provider.name(),
			medianOffset
		);
	}

	/**
	 * 여러 번 측정 후 RTT 기준 필터링 + 중앙값(offset)
	 */
	private long measureMedianOffset(TicketProvider provider) {

		List<ServerTimeMeasureResult> results = new ArrayList<>();

		for (int i = 0; i < MEASURE_COUNT; i++) {
			try {
				results.add(measureOnce(provider));
			} catch (Exception e) {
				log.warn("[SERVER TIME] measure failed: {}", e.getMessage());
			}
		}

		if (results.size() < MIN_VALID_SAMPLE) {
			throw new IllegalStateException("Not enough valid server time samples");
		}

		results.sort(Comparator.comparingLong(ServerTimeMeasureResult::rttMillis));

		List<Long> offsets = results.stream()
			.limit(results.size() - 1)
			.map(ServerTimeMeasureResult::offsetMillis)
			.sorted()
			.toList();

		return offsets.get(offsets.size() / 2);
	}

	/**
	 * 서버 시간 측정
	 */
	private ServerTimeMeasureResult measureOnce(TicketProvider provider) {

		long t1Nano = System.nanoTime();
		long t1Millis = System.currentTimeMillis();

		ResponseEntity<Void> response = restTemplate.exchange(
			provider.getUrl(),
			HttpMethod.HEAD,
			null,
			Void.class
		);

		long t2Nano = System.nanoTime();
		long rttMillis = (t2Nano - t1Nano) / 1_000_000;

		long tMidMillis = t1Millis + (rttMillis / 2);

		String dateHeader = response.getHeaders().getFirst(HttpHeaders.DATE);
		if (dateHeader == null) {
			throw new IllegalStateException("Date header missing");
		}

		Instant serverInstant = DateTimeFormatter.RFC_1123_DATE_TIME
			.parse(dateHeader, Instant::from);

		long offset = serverInstant.toEpochMilli() - tMidMillis;

		return new ServerTimeMeasureResult(offset, rttMillis);
	}
}
