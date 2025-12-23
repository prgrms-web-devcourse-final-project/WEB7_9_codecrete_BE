package com.back.web7_9_codecrete_be.domain.concerts.repository;

import com.back.web7_9_codecrete_be.domain.concerts.dto.concert.ConcertDetailResponse;
import com.back.web7_9_codecrete_be.domain.concerts.dto.concert.ConcertItem;
import com.back.web7_9_codecrete_be.domain.concerts.dto.concert.ListSort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.*;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ConcertRedisRepository {
    private final RedisTemplate<String, String> redisTemplate;
    private final RedisTemplate<String, Object> objectRedisTemplate;

    private static final String LOCK_FLAG_PREFIX = "initLoad: ";

    private static final String CONCERT_DETAIL_PREFIX = "concertDetail: ";

    private static final String CONCERT_LIST_PREFIX = "concertList: ";

    private static final String VIEW_COUNT_MAP = "viewCountMap";

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

    // 공연 목록 가져오기
    public List<ConcertItem> getConcertsList(Pageable pageable, ListSort sort) {
        String key = CONCERT_LIST_PREFIX + sort.name() + pageable.getPageNumber();
        Object object = objectRedisTemplate.opsForValue().get(key);
        List<ConcertItem> list = (List<ConcertItem>) object;
        if (list == null || list.isEmpty()) return List.of(); // null 이 아닌 empty 값 반환
        return list;
    }

    // 캐싱된 모든 공연 목록 삭제
    public void deleteAllConcertsList() {
        deleteAllItemsByPREFIX(CONCERT_LIST_PREFIX);
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
        if (concertDetailResponse == null) return null;
        int viewCount = concertDetailResponse.getViewCount();
        viewCountSet(concertId, viewCount + 1);
        concertDetailResponse.setViewCount(viewCount + 1);
        detailSave(concertId, concertDetailResponse);
        return concertDetailResponse;
    }

    // 공연 상세 삭제
    public void deleteDetail(String concertId) {
        redisTemplate.delete(CONCERT_DETAIL_PREFIX + concertId);
    }

    // 모든 공연 상세 삭제
    public void deleteAllConcertDetail() {
        deleteAllItemsByPREFIX(CONCERT_DETAIL_PREFIX);
    }

    // 조회수 처리 -> 좀 지저분한데 개선 여지 찾아보기
    public int viewCountSet(long concertId, int viewCount) {
        Map<String, Integer> rawMap = (Map<String, Integer>) objectRedisTemplate.opsForValue().get(VIEW_COUNT_MAP);

        if (rawMap == null) {
            Map<Long, Integer> viewCountMap = new HashMap<>();
            viewCountMap.put(concertId, viewCount);
            objectRedisTemplate.opsForValue().set(VIEW_COUNT_MAP, viewCountMap);
        } else {
            Map<Long, Integer> viewCountMap = convertViewCountMap(rawMap);
            viewCountMap.put(concertId, viewCount);
            objectRedisTemplate.opsForValue().set(VIEW_COUNT_MAP, viewCountMap);
            log.info(viewCountMap.size() + "view count size.");
        }
        return viewCount;
    }

    // 조회수 맵 조회
    public Map<Long, Integer> getViewCountMap() {
        Map<String, Integer> rawMap = (Map<String, Integer>) objectRedisTemplate.opsForValue().get(VIEW_COUNT_MAP);
        if (rawMap == null) return null;
        objectRedisTemplate.delete(VIEW_COUNT_MAP);
        return convertViewCountMap(rawMap);
    }

    // 조회수 맵 삭제
    public void deleteViewCountMap() {
        objectRedisTemplate.delete(VIEW_COUNT_MAP);
    }

    // String Integer 타입 맵을 Long Integer로 변환
    private Map<Long, Integer> convertViewCountMap(Map<String, Integer> rawMap) {
        Map<Long, Integer> viewCountMap = new HashMap<>();
        for (Map.Entry<String, Integer> stringIntegerEntry : rawMap.entrySet()) {
            viewCountMap.put(Long.parseLong(stringIntegerEntry.getKey()), stringIntegerEntry.getValue());
        }
        return viewCountMap;
    }

    // 해당 접두어의 모든 항목 삭제
    private void deleteAllItemsByPREFIX(String prefix) {
        String pattern = CONCERT_LIST_PREFIX + "*";
        ScanOptions options = ScanOptions.scanOptions().match(pattern).count(100).build();
        Set<String> keys = redisTemplate.execute((RedisCallback<Set<String>>) connection -> {
                    Set<String> keySet = new HashSet<>();
                    try (Cursor<byte[]> cursor = connection.scan(options)) {
                        while (cursor.hasNext()) {
                            keySet.add(new String(cursor.next()));
                        }
                    }
                    return keySet;
                }
        );

        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.info("Successfully deleted %s items with prefix: %s".formatted(keys.size() + "", prefix));
        } else {
            log.info("no items with prefix: %s".formatted(prefix));
        }
    }


}
