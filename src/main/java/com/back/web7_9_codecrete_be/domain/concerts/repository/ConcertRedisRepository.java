package com.back.web7_9_codecrete_be.domain.concerts.repository;

import com.back.web7_9_codecrete_be.domain.concerts.dto.concert.ConcertDetailResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Repository
@RequiredArgsConstructor
public class ConcertRedisRepository {
    private final RedisTemplate<String,String> redisTemplate;
    private final RedisTemplate<String, Object> detailRedisTemplate;

    private static final String LOCK_FLAG_PREFIX = "initLoad: ";

    private static final String CONCERT_DETAIL_PREFIX = "concertDetail: ";

    private static final int HOUR = 3600;

    // 최초 공연 로드 락
    public void lockSave(String key, String value) {
        redisTemplate.opsForValue().set(
                LOCK_FLAG_PREFIX + key,
                value,
                900,
                TimeUnit.SECONDS);
    }

    public String lockGet(String key) {
        return redisTemplate.opsForValue().get(LOCK_FLAG_PREFIX + key);
    }

    public void unlockSave(String key) {
        redisTemplate.delete(LOCK_FLAG_PREFIX + key);
    }

    // 공연 상세 캐싱
    public void detailSave(long concertId, ConcertDetailResponse concertDetailResponse) {
        detailRedisTemplate.opsForValue().set(
                CONCERT_DETAIL_PREFIX + concertId,
                concertDetailResponse,
                HOUR,
                TimeUnit.SECONDS
        );
    }

    // todo : 객체 일부의 값만 바뀌는거니 해당 값만 바꿔서 저장하거나 Redis 내부의 값만 갱신할 수 있는 방법 찾기
    public ConcertDetailResponse getDetail(long concertId) {
        ConcertDetailResponse concertDetailResponse = (ConcertDetailResponse) detailRedisTemplate.opsForValue().get(CONCERT_DETAIL_PREFIX + concertId);
        concertDetailResponse.setViewCount(concertDetailResponse.getViewCount() + 1);
        detailSave(concertId, concertDetailResponse);
        return concertDetailResponse;
    }

    public void deleteDetail(String concertId) { redisTemplate.delete(CONCERT_DETAIL_PREFIX + concertId); }

}
