package com.back.web7_9_codecrete_be.domain.artists.service.spotify.cache;

import com.back.web7_9_codecrete_be.domain.artists.dto.response.SpotifyArtistDetailCache;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;


@Slf4j
@Service
@RequiredArgsConstructor
// Spotify 아티스트 상세 정보 Redis 캐시 관리 서비스 : 성능 최적화를 위한 캐시 전략 담당
public class SpotifyCacheService {

    private final RedisTemplate<String, String> redisTemplate;
    private final RedisTemplate<String, Object> objectRedisTemplate;
    private final ObjectMapper objectMapper;

    private static final String CACHE_KEY_PREFIX = "artist:detail:spotify:";
    private static final String LOCK_KEY_PREFIX = "artist:detail:spotify:lock:";
    private static final long CACHE_TTL_SECONDS = 3600; // 1시간 (기본값, 추후 3~6시간 조정 가능)
    private static final long LOCK_TTL_SECONDS = 30; // 락 TTL: 30초 (API 호출 완료 대기 시간)

    // Redis 캐시에서 Spotify 상세 정보 조회
    public SpotifyArtistDetailCache getCached(String spotifyArtistId) {
        try {
            String cacheKey = getCacheKey(spotifyArtistId);
            Object cached = objectRedisTemplate.opsForValue().get(cacheKey);

            if (cached == null) {
                return null;
            }

            // Object를 SpotifyArtistDetailCache로 변환
            if (cached instanceof SpotifyArtistDetailCache) {
                return (SpotifyArtistDetailCache) cached;
            }

            // LinkedHashMap 등으로 역직렬화된 경우 ObjectMapper로 변환
            return objectMapper.convertValue(cached, SpotifyArtistDetailCache.class);
        } catch (Exception e) {
            log.warn("Redis 캐시 조회 실패: spotifyArtistId={}", spotifyArtistId, e);
            return null;
        }
    }

    // Redis 캐시에 Spotify 상세 정보 저장
    public void save(String spotifyArtistId, SpotifyArtistDetailCache data) {
        try {
            String cacheKey = getCacheKey(spotifyArtistId);
            objectRedisTemplate.opsForValue().set(
                    cacheKey,
                    data,
                    CACHE_TTL_SECONDS,
                    TimeUnit.SECONDS
            );
            log.debug("Spotify 상세 정보 캐시 저장: spotifyArtistId={}, ttl={}초", spotifyArtistId, CACHE_TTL_SECONDS);
        } catch (Exception e) {
            log.warn("Redis 캐시 저장 실패: spotifyArtistId={}", spotifyArtistId, e);
            // 캐시 저장 실패해도 API 호출은 성공했으므로 계속 진행
        }
    }

    // 캐시 스탬피드 방지: Redis 락을 사용하여 동시 API 호출 제한
    public SpotifyArtistDetailCache getOrFetchWithLock(
            String spotifyArtistId,
            java.util.function.Supplier<SpotifyArtistDetailCache> apiCallSupplier
    ) {
        String lockKey = getLockKey(spotifyArtistId);

        // 락 획득 시도 (SETNX 방식)
        Boolean lockAcquired = redisTemplate.opsForValue().setIfAbsent(
                lockKey,
                "locked",
                LOCK_TTL_SECONDS,
                TimeUnit.SECONDS
        );

        if (Boolean.TRUE.equals(lockAcquired)) {
            // 락 획득 성공: 이 스레드가 API 호출 담당
            try {
                log.debug("Spotify API 호출 락 획득: spotifyArtistId={}", spotifyArtistId);

                // 다시 한 번 캐시 확인 (락 획득 대기 중 다른 스레드가 저장했을 수 있음)
                SpotifyArtistDetailCache doubleCheck = getCached(spotifyArtistId);
                if (doubleCheck != null) {
                    log.debug("락 획득 후 캐시 재조회 HIT: spotifyArtistId={}", spotifyArtistId);
                    return doubleCheck;
                }

                // Spotify API 호출
                SpotifyArtistDetailCache spotifyData = apiCallSupplier.get();

                // 캐시에 저장
                save(spotifyArtistId, spotifyData);

                return spotifyData;
            } finally {
                // 락 해제
                redisTemplate.delete(lockKey);
            }
        } else {
            // 락 획득 실패: 다른 스레드가 API 호출 중
            log.debug("Spotify API 호출 락 획득 실패 (다른 스레드가 처리 중): spotifyArtistId={}", spotifyArtistId);

            // 짧은 대기 후 캐시 재조회 (다른 스레드가 저장 완료했을 수 있음)
            try {
                Thread.sleep(100); // 100ms 대기
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // 캐시 재조회
            SpotifyArtistDetailCache retryCache = getCached(spotifyArtistId);
            if (retryCache != null) {
                log.debug("락 대기 후 캐시 재조회 HIT: spotifyArtistId={}", spotifyArtistId);
                return retryCache;
            }

            // 여전히 캐시가 없으면 최대 3초까지 대기하며 재시도
            int maxRetries = 30; // 100ms * 30 = 3초
            for (int i = 0; i < maxRetries; i++) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }

                retryCache = getCached(spotifyArtistId);
                if (retryCache != null) {
                    log.debug("락 대기 중 캐시 재조회 HIT ({}ms 후): spotifyArtistId={}", (i + 1) * 100, spotifyArtistId);
                    return retryCache;
                }
            }

            // 최종적으로도 캐시가 없으면 직접 API 호출 (락이 만료되었을 수 있음)
            log.warn("락 대기 후에도 캐시 없음, 직접 API 호출: spotifyArtistId={}", spotifyArtistId);
            SpotifyArtistDetailCache spotifyData = apiCallSupplier.get();
            save(spotifyArtistId, spotifyData);
            return spotifyData;
        }
    }

    private String getCacheKey(String spotifyArtistId) {
        return CACHE_KEY_PREFIX + spotifyArtistId;
    }

    private String getLockKey(String spotifyArtistId) {
        return LOCK_KEY_PREFIX + spotifyArtistId;
    }
}

