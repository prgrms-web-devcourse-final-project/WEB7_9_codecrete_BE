package com.back.web7_9_codecrete_be.domain.artists.service.spotifyService;

import com.back.web7_9_codecrete_be.global.spotify.SpotifyClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

// 429 에러 처리 및 전역 쿨다운 관리
// 401 에러 처리 (토큰 만료 시 자동 재발급)

@Slf4j
@Component
@RequiredArgsConstructor
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
    
    public SimpleRateLimiter getSpotifyRateLimiter() {
        return spotifyRateLimiter;
    }
    
    public SimpleRateLimiter getMusicBrainzRateLimiter() {
        return musicBrainzRateLimiter;
    }
    
    /**
     * Rate Limit 재시도 로직 (429 에러 처리)
     * 429면 Retry-After 헤더 확인 후 대기하고 재시도 (최대 3회)
     * 연속 429 발생 시 전역 쿨다운 적용
     * 
     * @param supplier API 호출 함수
     * @param context 컨텍스트 정보 (로깅용)
     * @return API 호출 결과
     * @throws RuntimeException 재시도 모두 실패 시
     */
    public <T> T callWithRateLimitRetry(java.util.function.Supplier<T> supplier, String context) {
        final int maxRetry = 3;
        
        for (int attempt = 1; attempt <= maxRetry; attempt++) {
            // 전역 쿨다운 확인
            long now = System.currentTimeMillis();
            if (globalCooldownUntil > now) {
                long remainingSec = (globalCooldownUntil - now) / 1000;
                log.warn("전역 쿨다운 중: {}초 남음. API 호출 대기", remainingSec);
                try {
                    Thread.sleep(remainingSec * 1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(context + ": interrupted during global cooldown", ie);
                }
                // 쿨다운이 끝났으면 카운터 리셋
                if (System.currentTimeMillis() >= globalCooldownUntil) {
                    globalConsecutive429Count = 0;
                }
            }
            
            try {
                T result = supplier.get();
                // 성공 시 전역 429 카운터 리셋
                globalConsecutive429Count = 0;
                return result;
            } catch (Exception e) {
                // 401 에러 확인 (Unauthorized - 토큰 만료)
                boolean is401 = is401Error(e);
                
                if (is401) {
                    log.warn("401 Unauthorized 에러 감지 - 토큰 재발급 후 재시도: attempt={}/{}", attempt, maxRetry);
                    // 토큰 강제 재발급
                    spotifyClient.forceRefreshToken();
                    
                    if (attempt < maxRetry) {
                        // 재시도
                        continue;
                    } else {
                        log.error("401 에러 재시도 횟수 초과 ({}회)", maxRetry);
                        throw new RuntimeException(context + ": 401 Unauthorized - 토큰 재발급 후에도 실패", e);
                    }
                }
                // 원본 예외 확인 (래핑된 경우 cause 확인)
                Throwable originalException = e;
                Throwable current = e;
                
                // 예외 체인을 따라가며 TooManyRequestsException 찾기
                while (current != null) {
                    String currentClassName = current.getClass().getSimpleName();
                    if (currentClassName.contains("TooManyRequests")) {
                        originalException = current;
                        break;
                    }
                    current = current.getCause();
                }
                
                // 래핑된 경우 cause 확인
                if (originalException == e && e instanceof RuntimeException && e.getCause() != null) {
                    originalException = e.getCause();
                }
                
                String className = originalException.getClass().getSimpleName();
                String errorMsg = originalException.getMessage();
                String wrapperClassName = e.getClass().getSimpleName();
                String wrapperErrorMsg = e.getMessage();
                
                // 429 에러 확인 (원본 예외와 래핑된 예외 모두 확인)
                boolean is429 = className.contains("TooManyRequests") || 
                               (errorMsg != null && (errorMsg.contains("429") || errorMsg.contains("Too Many Requests"))) ||
                               wrapperClassName.contains("TooManyRequests") ||
                               (wrapperErrorMsg != null && (wrapperErrorMsg.contains("429") || wrapperErrorMsg.contains("Too Many Requests")));
                
                // 예외 체인 전체 확인
                if (!is429) {
                    current = e;
                    while (current != null) {
                        String currentClassName = current.getClass().getSimpleName();
                        String currentMsg = current.getMessage();
                        if (currentClassName.contains("TooManyRequests") || 
                            (currentMsg != null && (currentMsg.contains("429") || currentMsg.contains("Too Many Requests")))) {
                            is429 = true;
                            originalException = current;
                            break;
                        }
                        current = current.getCause();
                    }
                }
                
                // 디버깅 로그
                if (is429) {
                    log.warn("429 에러 감지: attempt={}/{}, className={}, wrapperClassName={}, errorMsg={}", 
                            attempt, maxRetry, className, wrapperClassName, errorMsg);
                } else {
                    log.debug("예외 발생 (429 아님): attempt={}/{}, className={}, wrapperClassName={}, errorMsg={}", 
                            attempt, maxRetry, className, wrapperClassName, errorMsg);
                }
                
                if (is429) {
                    // 전역 429 카운터 증가
                    synchronized (this) {
                        globalConsecutive429Count++;
                        
                        // 연속 429 발생 시 전역 쿨다운 활성화
                        if (globalConsecutive429Count >= MAX_CONSECUTIVE_429) {
                            globalCooldownUntil = System.currentTimeMillis() + GLOBAL_COOLDOWN_DURATION_MS;
                            log.error("전역 연속 {}회 429 발생 → 전역 쿨다운 {}초 활성화", 
                                    globalConsecutive429Count, GLOBAL_COOLDOWN_DURATION_MS / 1000);
                        }
                    }
                    
                    if (attempt < maxRetry) {
                        // Retry-After 헤더는 원본 예외에서 추출
                        Throwable headerException = originalException;
                        // Retry-After 헤더 추출 시도
                        long waitSec = 5; // 기본값 5초 (더 보수적으로)
                        final long minWaitSec = 3; // 최소 대기 시간 3초
                        final long bufferSec = 2; // 여유 시간 2초 추가
                    
                    try {
                        // Spotify SDK의 예외에서 Retry-After 헤더 추출 시도
                        // reflection을 통해 헤더 정보 확인 (원본 예외에서)
                        java.lang.reflect.Method getHeadersMethod = null;
                        try {
                            getHeadersMethod = headerException.getClass().getMethod("getResponseHeaders");
                        } catch (NoSuchMethodException ignored) {
                            // 메서드가 없으면 기본값 사용
                        }
                        
                        if (getHeadersMethod != null) {
                            try {
                                Object headers = getHeadersMethod.invoke(headerException);
                                if (headers != null && headers instanceof java.util.Map) {
                                    @SuppressWarnings("unchecked")
                                    java.util.Map<String, java.util.List<String>> headerMap = 
                                            (java.util.Map<String, java.util.List<String>>) headers;
                                    java.util.List<String> retryAfterList = headerMap.get("Retry-After");
                                    if (retryAfterList != null && !retryAfterList.isEmpty()) {
                                        try {
                                            long headerValue = Long.parseLong(retryAfterList.get(0));
                                            // 헤더 값 + 여유 시간, 최소값 보장
                                            waitSec = Math.max(minWaitSec, headerValue + bufferSec);
                                        } catch (NumberFormatException ignored) {
                                            // 파싱 실패 시 기본값 사용
                                        }
                                    }
                                }
                            } catch (Exception ignored) {
                                // 헤더 추출 실패 시 기본값 사용
                            }
                        }
                    } catch (Exception ignored) {
                        // reflection 실패 시 기본값 사용
                    }
                    
                        log.warn("{}: 429 Too Many Requests. attempt={}/{}. retry-after={}s (헤더값+{}초 여유)", 
                                context, attempt, maxRetry, waitSec, bufferSec);
                        
                        try {
                            Thread.sleep(waitSec * 1000L);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw new RuntimeException(context + ": interrupted during retry wait", ie);
                        }
                        
                        // 재시도
                        continue;
                    } else {
                        // 재시도 횟수 초과
                        log.error("{}: 429 Too Many Requests. 재시도 횟수 초과 ({}회)", context, maxRetry);
                    }
                }
                
                // 429가 아니거나 재시도 횟수 초과 시 예외 재throw
                // IOException 등은 그대로 throw (호출부에서 처리)
                throw e;
            }
        }
        
        throw new RuntimeException(context + ": rate limit retry exhausted");
    }
    
    // 401 에러인지 확인
    private boolean is401Error(Throwable e) {
        Throwable current = e;
        while (current != null) {
            String className = current.getClass().getSimpleName();
            String errorMsg = current.getMessage();
            
            // 401 에러 확인
            if (className.contains("Unauthorized") || 
                className.contains("401") ||
                (errorMsg != null && (errorMsg.contains("401") || 
                                     errorMsg.contains("Unauthorized") ||
                                     errorMsg.contains("Invalid access token")))) {
                return true;
            }
            
            current = current.getCause();
        }
        return false;
    }
}

