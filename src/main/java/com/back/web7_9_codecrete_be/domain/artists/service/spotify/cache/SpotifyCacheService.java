package com.back.web7_9_codecrete_be.domain.artists.service.spotify.cache;

import com.back.web7_9_codecrete_be.domain.artists.dto.response.SpotifyArtistDetailCache;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

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
    private static final long CACHE_TTL_SECONDS = 3600; // 1시간
    private static final long LOCK_TTL_SECONDS = 30;    // 30초

    private static final long RETRY_SLEEP_MS = 100;
    private static final int MAX_RETRIES = 30; // 100ms * 30 = 3초

    public SpotifyArtistDetailCache getCached(String spotifyArtistId) {
        try {
            String cacheKey = getCacheKey(spotifyArtistId);
            Object cached = objectRedisTemplate.opsForValue().get(cacheKey);

            if (cached == null) return null;

            if (cached instanceof SpotifyArtistDetailCache) {
                return (SpotifyArtistDetailCache) cached;
            }

            return objectMapper.convertValue(cached, SpotifyArtistDetailCache.class);
        } catch (Exception e) {
            log.error("Redis 캐시 조회 실패: spotifyArtistId={}", spotifyArtistId, e);
            return null;
        }
    }

    public void save(String spotifyArtistId, SpotifyArtistDetailCache data) {
        if (data == null) return; // (선택) supplier가 null 반환 시 방어

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
        }
    }

    public SpotifyArtistDetailCache getOrFetchWithLock(
            String spotifyArtistId,
            Supplier<SpotifyArtistDetailCache> apiCallSupplier
    ) {
        String lockKey = getLockKey(spotifyArtistId);

        // ✅ 핵심 수정 1) 락 획득 자체가 예외면 fallback
        final Boolean lockAcquired;
        try {
            lockAcquired = redisTemplate.opsForValue().setIfAbsent(
                    lockKey,
                    "locked",
                    LOCK_TTL_SECONDS,
                    TimeUnit.SECONDS
            );
        } catch (Exception e) {
            log.warn("Redis 락 획득 실패(예외) → 직접 API 호출로 fallback: spotifyArtistId={}", spotifyArtistId, e);
            SpotifyArtistDetailCache spotifyData = apiCallSupplier.get();
            save(spotifyArtistId, spotifyData);
            return spotifyData;
        }

        if (Boolean.TRUE.equals(lockAcquired)) {
            try {
                log.debug("Spotify API 호출 락 획득: spotifyArtistId={}", spotifyArtistId);

                // Double-check
                SpotifyArtistDetailCache doubleCheck = getCached(spotifyArtistId);
                if (doubleCheck != null) {
                    log.debug("락 획득 후 캐시 재조회 HIT: spotifyArtistId={}", spotifyArtistId);
                    return doubleCheck;
                }

                SpotifyArtistDetailCache spotifyData = apiCallSupplier.get();
                save(spotifyArtistId, spotifyData);
                return spotifyData;

            } finally {
                // ✅ 핵심 수정 2) 락 해제도 예외 방어
                try {
                    redisTemplate.delete(lockKey);
                } catch (Exception e) {
                    log.warn("Redis 락 해제 실패: lockKey={}", lockKey, e);
                }
            }
        }

        // 락 획득 실패: 다른 스레드가 API 호출 중
        log.debug("Spotify API 호출 락 획득 실패 (다른 스레드가 처리 중): spotifyArtistId={}", spotifyArtistId);

        sleepQuietly(RETRY_SLEEP_MS);

        SpotifyArtistDetailCache retryCache = getCached(spotifyArtistId);
        if (retryCache != null) {
            log.debug("락 대기 후 캐시 재조회 HIT: spotifyArtistId={}", spotifyArtistId);
            return retryCache;
        }

        for (int i = 0; i < MAX_RETRIES; i++) {
            sleepQuietly(RETRY_SLEEP_MS);

            retryCache = getCached(spotifyArtistId);
            if (retryCache != null) {
                log.debug("락 대기 중 캐시 재조회 HIT ({}ms 후): spotifyArtistId={}", (i + 1) * RETRY_SLEEP_MS, spotifyArtistId);
                return retryCache;
            }
        }

        // 최종 fallback
        log.warn("락 대기 후에도 캐시 없음, 직접 API 호출: spotifyArtistId={}", spotifyArtistId);
        SpotifyArtistDetailCache spotifyData = apiCallSupplier.get();
        save(spotifyArtistId, spotifyData);
        return spotifyData;
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private String getCacheKey(String spotifyArtistId) {
        return CACHE_KEY_PREFIX + spotifyArtistId;
    }

    private String getLockKey(String spotifyArtistId) {
        return LOCK_KEY_PREFIX + spotifyArtistId;
    }
}
