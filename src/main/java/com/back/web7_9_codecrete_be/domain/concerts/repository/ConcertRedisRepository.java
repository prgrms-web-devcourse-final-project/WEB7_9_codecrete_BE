package com.back.web7_9_codecrete_be.domain.concerts.repository;

import com.back.web7_9_codecrete_be.domain.concerts.dto.concert.ConcertDetailResponse;
import com.back.web7_9_codecrete_be.domain.concerts.dto.concert.ConcertItem;
import com.back.web7_9_codecrete_be.domain.concerts.dto.concert.ListSort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Repository
@RequiredArgsConstructor
public class ConcertRedisRepository {
    private final RedisTemplate<String, String> redisTemplate;
    private final RedisTemplate<String, Object> objectRedisTemplate;

    private static final String LOCK_FLAG_PREFIX = "initLoad: ";

    private static final String CONCERT_DETAIL_PREFIX = "concertDetail: ";

    private static final String CONCERT_LIST_PREFIX = "concertList: ";

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

    // 공연 목록 캐싱
    public void listSave(ListSort sort, Pageable pageable, List<ConcertItem> list) {
        String key = CONCERT_LIST_PREFIX + sort.name() + pageable.getPageNumber();
        objectRedisTemplate.opsForValue().set(key, list, HOUR, TimeUnit.SECONDS);
    }

    public List<ConcertItem> getConcertsList(Pageable pageable, ListSort sort) {
        String key = CONCERT_LIST_PREFIX + sort.name() + pageable.getPageNumber();
        Object object = objectRedisTemplate.opsForValue().get(key);
        List<ConcertItem> list = (List<ConcertItem>) object;
        if( list == null || list.isEmpty())  return List.of(); // null 이 아닌 empty 값 반환
        return list;
    }

    // 공연 상세 캐싱
    public void detailSave(long concertId, ConcertDetailResponse concertDetailResponse) {
        objectRedisTemplate.opsForValue().set(
                CONCERT_DETAIL_PREFIX + concertId,
                concertDetailResponse,
                HOUR,
                TimeUnit.SECONDS
        );
    }

    // todo : 객체 일부의 값만 바뀌는거니 해당 값만 바꿔서 저장하거나 Redis 내부의 값만 갱신할 수 있는 방법 찾기
    public ConcertDetailResponse getDetail(long concertId) {
        ConcertDetailResponse concertDetailResponse = (ConcertDetailResponse) objectRedisTemplate.opsForValue().get(CONCERT_DETAIL_PREFIX + concertId);
        concertDetailResponse.setViewCount(concertDetailResponse.getViewCount() + 1);
        detailSave(concertId, concertDetailResponse);
        return concertDetailResponse;
    }

    public void deleteDetail(String concertId) {
        redisTemplate.delete(CONCERT_DETAIL_PREFIX + concertId);
    }

}
