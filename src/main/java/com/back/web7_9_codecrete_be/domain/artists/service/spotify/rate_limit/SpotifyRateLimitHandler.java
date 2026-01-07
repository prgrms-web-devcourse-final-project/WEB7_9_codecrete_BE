package com.back.web7_9_codecrete_be.domain.artists.service.spotify.rate_limit;

import com.back.web7_9_codecrete_be.global.spotify.SpotifyClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;



@Slf4j
@Component
@RequiredArgsConstructor
// 429 에러 처리 및 전역 쿨다운 관리
// 401 에러 처리 (토큰 만료 시 자동 재발급)
public class SpotifyRateLimitHandler {
    
    private final SpotifyClient spotifyClient;
    
    private static final long SPOTIFY_RATE_LIMIT_INTERVAL_MS = 500; // 초당 2회
    private static final long MUSICBRAINZ_RATE_LIMIT_INTERVAL_MS = 1000; // 초당 1회
    private static final long GLOBAL_COOLDOWN_DURATION_MS = 60_000; // 60초
    private static final int MAX_CONSECUTIVE_429 = 3; // 연속 3회 429 발생 시 쿨다운
    
    private final SimpleRateLimiter spotifyRateLimiter = new SimpleRateLimiter(SPOTIFY_RATE_LIMIT_INTERVAL_MS);
    private final SimpleRateLimiter musicBrainzRateLimiter = new SimpleRateLimiter(MUSICBRAINZ_RATE_LIMIT_INTERVAL_MS);
    
    private volatile long globalCooldownUntil = 0;
    private volatile int globalConsecutive429Count = 0;
    
    // Rate Limit 및 401 에러를 처리하며 API 호출 실행
    public <T> T callWithRateLimitRetry(java.util.function.Supplier<T> apiCall, String context) {
        int maxRetry = 3;
        int retryCount = 0;
        
        while (retryCount < maxRetry) {
            try {
                // 전역 쿨다운 확인
                waitForGlobalCooldown();
                
                // API 호출 실행
                T result = apiCall.get();
                
                // 성공 시 연속 429 카운트 리셋
                globalConsecutive429Count = 0;
                
                return result;
                
            } catch (RuntimeException e) {
                String errorMessage = e.getMessage();
                
                // 401 Unauthorized 에러 처리 (토큰 만료)
                if (errorMessage != null && errorMessage.contains("401")) {
                    log.warn("Spotify API 401 에러 발생 (토큰 만료): {}, 토큰 재발급 후 재시도", context);
                    spotifyClient.forceRefreshToken();
                    
                    // 재시도
                    retryCount++;
                    if (retryCount >= maxRetry) {
                        log.error("Spotify API 401 에러 재시도 모두 실패: {}", context);
                        throw new RuntimeException("rate limit retry exhausted: " + context, e);
                    }
                    
                    try {
                        Thread.sleep(1000 * retryCount); // 지수 백오프
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Interrupted during retry", ie);
                    }
                    continue;
                }
                
                // 429 Too Many Requests 에러 처리
                if (errorMessage != null && errorMessage.contains("429")) {
                    globalConsecutive429Count++;
                    log.warn("Spotify API 429 에러 발생 (재시도 {}/{}): {}", 
                            retryCount + 1, maxRetry, context);
                    
                    // 연속 3회 429 발생 시 전역 쿨다운 활성화
                    if (globalConsecutive429Count >= MAX_CONSECUTIVE_429) {
                        log.error("Spotify API 연속 429 에러 {}회 발생, 전역 쿨다운 {}초 활성화", 
                                MAX_CONSECUTIVE_429, GLOBAL_COOLDOWN_DURATION_MS / 1000);
                        activateGlobalCooldown();
                        globalConsecutive429Count = 0;
                    }
                    
                    // 재시도
                    retryCount++;
                    if (retryCount >= maxRetry) {
                        log.error("Spotify API 429 에러 재시도 모두 실패: {}", context);
                        throw new RuntimeException("rate limit retry exhausted: " + context, e);
                    }
                    
                    // 지수 백오프: 1초, 2초, 4초
                    long backoffMs = (long) Math.pow(2, retryCount - 1) * 1000;
                    try {
                        Thread.sleep(backoffMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Interrupted during retry", ie);
                    }
                    continue;
                }
                
                // 429, 401이 아닌 다른 에러는 즉시 throw
                throw e;
            }
        }
        
        throw new RuntimeException("rate limit retry exhausted: " + context);
    }
    
    // 전역 쿨다운이 활성화되어 있으면 대기
    private void waitForGlobalCooldown() {
        long now = System.currentTimeMillis();
        if (now < globalCooldownUntil) {
            long waitMs = globalCooldownUntil - now;
            log.info("전역 쿨다운 대기 중: {}ms 남음", waitMs);
            try {
                Thread.sleep(waitMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted during global cooldown", e);
            }
        }
    }
    
    // 전역 쿨다운 활성화
    private void activateGlobalCooldown() {
        globalCooldownUntil = System.currentTimeMillis() + GLOBAL_COOLDOWN_DURATION_MS;
    }
}

