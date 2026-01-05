package com.back.web7_9_codecrete_be.domain.concerts.repository;

import com.back.web7_9_codecrete_be.domain.concerts.dto.concert.ConcertDetailResponse;
import com.back.web7_9_codecrete_be.domain.concerts.dto.concert.ConcertItem;
import com.back.web7_9_codecrete_be.domain.concerts.dto.concert.ListSort;
import com.back.web7_9_codecrete_be.domain.users.entity.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.*;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ConcertRedisRepository {
    private final RedisTemplate<String, String> redisTemplate;
    private final RedisTemplate<String, Object> objectRedisTemplate;
    private final ObjectMapper objectMapper;

    private static final String LOCK_FLAG_PREFIX = "initLoad:";

    private static final String CONCERT_DETAIL_PREFIX = "concertDetail:";

    private static final String CONCERT_LIST_PREFIX = "concertList:";

    private static final String CONCERTS_COUNT_PREFIX = "totalConcertsCount:";

    private static final String CONCERTS_VIEW_COUNTS = "concertsViewCount";

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
    public void saveConcertsList(ListSort sort, Pageable pageable, List<ConcertItem> list) {
        String key = CONCERT_LIST_PREFIX + sort.name() + pageable.getPageNumber() + "S" + pageable.getPageSize();
        objectRedisTemplate.opsForValue().set(key, list, HOUR, TimeUnit.SECONDS);
    }

    // 공연 목록 가져오기
    public List<ConcertItem> getConcertsList(Pageable pageable, ListSort sort) {
        String key = CONCERT_LIST_PREFIX + sort.name() + pageable.getPageNumber()+ "S" + pageable.getPageSize();
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
    public void saveConcertDetail(Long concertId, ConcertDetailResponse concertDetailResponse) {
        objectRedisTemplate.opsForValue().set(
                CONCERT_DETAIL_PREFIX + concertId,
                concertDetailResponse,
                2,
                TimeUnit.DAYS
        );
        redisTemplate.opsForHash().put(CONCERTS_VIEW_COUNTS, concertId.toString(), concertDetailResponse.getViewCount() + "");
    }

    // 공연 정보 가져오기
    public ConcertDetailResponse getCachedConcertDetail(Long concertId) {
        ConcertDetailResponse concertDetailResponse = getConcertDetailResponse(concertId);
        if (concertDetailResponse == null) return null;
        return concertDetailResponse;
    }

    private ConcertDetailResponse getConcertDetailResponse(long concertId) {
        Object rawObject = objectRedisTemplate.opsForValue().get(CONCERT_DETAIL_PREFIX + concertId);
        if (rawObject == null) return null;
        if(rawObject instanceof ConcertDetailResponse) return (ConcertDetailResponse) rawObject;
        return objectMapper.convertValue(rawObject, ConcertDetailResponse.class);
    }

    // 공연 상세 삭제
    public void deleteConcertDetail(String concertId) {
        redisTemplate.delete(CONCERT_DETAIL_PREFIX + concertId);
    }

    // 모든 공연 상세 삭제
    public void deleteAllCachedConcertDetail() {
        deleteAllItemsByPREFIX(CONCERT_DETAIL_PREFIX);
    }

    // 모든 공연의 조회수 맵 조회 -> 하나의 해시를 기준으로 가져올 수 있게 처리
    public Map<Long, Integer> getCachedViewCountMap() {
        Map<Object, Object> rawMap = redisTemplate.opsForHash().entries(CONCERTS_VIEW_COUNTS);
        Map<Long, Integer> viewCountMap = new HashMap<>();
        for (Map.Entry<Object, Object> rawEntity : rawMap.entrySet()) {
            Long concertID = Long.valueOf(rawEntity.getKey().toString());
            Integer viewCount = Integer.valueOf(rawEntity.getValue().toString());
            viewCountMap.put(concertID, viewCount);
        }
        return viewCountMap;
    }

    // 공연의 조회수 조회
    public Long getCachedViewCount(Long concertId) {
        Long viewCount = Long.valueOf(redisTemplate.opsForHash()
                .get(
                        CONCERTS_VIEW_COUNTS,
                        concertId.toString()
                )
                .toString());

        return viewCount;
    }

    // 캐시된 조회수 삭제
    public void deleteViewCount(Long concertId) {
        redisTemplate.opsForHash().delete(CONCERTS_VIEW_COUNTS, concertId.toString());
    }

    // 공연에 예매시작, 종료일자 추가
    public void updateCachedTickingDate(Long concertId, LocalDateTime TicketTime, LocalDateTime TicketEndTime) {
        ConcertDetailResponse concertDetailResponse = getConcertDetailResponse(concertId);
        if (concertDetailResponse == null) return;
        concertDetailResponse.setTicketTime(TicketTime);
        concertDetailResponse.setTicketEndTime(TicketEndTime);
        saveConcertDetail(concertId, concertDetailResponse);
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
        String pattern = prefix + "*";
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

    // 총 공연의 개수 저장
    public Long saveTotalConcertsCount(Long totalConcertsCount, ListSort sort) {
        redisTemplate.opsForValue().set(CONCERTS_COUNT_PREFIX + sort.name(), totalConcertsCount.toString());
        return totalConcertsCount;
    }

    // 총 공연의 개수 조회
    public Long getTotalConcertsCount(ListSort sort) {
        String raw = redisTemplate.opsForValue().get(CONCERTS_COUNT_PREFIX + sort.name());
        if (raw == null) return -1L;
        else return Long.parseLong(redisTemplate.opsForValue().get(CONCERTS_COUNT_PREFIX + sort.name()));
    }

    // 총 공연의 개수 삭제
    public void deleteTotalConcertsCount(ListSort sort) {
        redisTemplate.delete(CONCERTS_COUNT_PREFIX + sort.name());
    }

    // 캐시된 공연의 좋아요 정보 수정 -?
    public void upCountConcertLikeCountInConcertDetail(Long concertId) {
        ConcertDetailResponse concertDetailResponse = (ConcertDetailResponse)objectRedisTemplate.opsForValue().get(CONCERT_DETAIL_PREFIX + concertId.toString());
        if (concertDetailResponse == null) return;
        else  concertDetailResponse.setLikeCount(concertDetailResponse.getLikeCount()+1);
        objectRedisTemplate.opsForValue().set(CONCERT_DETAIL_PREFIX + concertId.toString(), concertDetailResponse);
    }

    // 사용자가 좋아요를 누른 공연의 개수 조회(임시 캐시 느낌으로 짧게 저장, 조회시 시간 갱신 ~1일
    public Long getUserLikedCount(User user) {
        String raw = redisTemplate.opsForValue().get(CONCERTS_COUNT_PREFIX + user.getId());
        if (raw == null) return -1L;
        redisTemplate.expire(CONCERTS_COUNT_PREFIX + user.getId(), 1, TimeUnit.DAYS);
        return Long.parseLong(redisTemplate.opsForValue().get(CONCERTS_COUNT_PREFIX + user.getId()));
    }

    // 사용자가 좋아요를 누른 공연의 개수 저장
    public Long saveUserLikedCount(User user, Long count) {
        redisTemplate.opsForValue().set(
                CONCERTS_COUNT_PREFIX + user.getId(),
                count.toString(),
                1,
                TimeUnit.DAYS
        );
        return count;
    }

    // 좋아요 누른 공연의 개수 캐시 삭제
    public void deleteUserLikedCount(User user) {
        redisTemplate.delete(CONCERTS_COUNT_PREFIX + user.getId());
    }


}
