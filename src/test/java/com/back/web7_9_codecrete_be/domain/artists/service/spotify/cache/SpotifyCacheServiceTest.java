package com.back.web7_9_codecrete_be.domain.artists.service.spotify.cache;

import com.back.web7_9_codecrete_be.domain.artists.dto.response.AlbumResponse;
import com.back.web7_9_codecrete_be.domain.artists.dto.response.SpotifyArtistDetailCache;
import com.back.web7_9_codecrete_be.domain.artists.dto.response.TopTrackResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@DisplayName("SpotifyCacheService Redis 캐시 테스트")
class SpotifyCacheServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private RedisTemplate<String, Object> objectRedisTemplate;

    @Mock
    private ValueOperations<String, String> stringValueOps;

    @Mock
    private ValueOperations<String, Object> objectValueOps;

    @Mock
    private ObjectMapper objectMapper;

    // ✅ @InjectMocks 제거 (RedisTemplate 2개 제네릭 주입 꼬임 방지)
    private SpotifyCacheService spotifyCacheService;

    // 서비스 상수와 동일하게 유지
    private static final long CACHE_TTL_SECONDS = 3600L;
    private static final long LOCK_TTL_SECONDS = 30L;

    private static final String SPOTIFY_ARTIST_ID = "test_spotify_id_123";
    private static final String CACHE_KEY = "artist:detail:spotify:" + SPOTIFY_ARTIST_ID;
    private static final String LOCK_KEY = "artist:detail:spotify:lock:" + SPOTIFY_ARTIST_ID;

    private SpotifyArtistDetailCache testCacheData;

    @BeforeEach
    void setUp() {
        spotifyCacheService = new SpotifyCacheService(redisTemplate, objectRedisTemplate, objectMapper);

        // ✅ getCached/save가 공통으로 사용하는 objectRedisTemplate만 기본 스텁 (불필요 stubbing 방지)
        when(objectRedisTemplate.opsForValue()).thenReturn(objectValueOps);

        testCacheData = new SpotifyArtistDetailCache(
                "Test Artist",
                "https://example.com/image.jpg",
                85.5,
                List.of(
                        new TopTrackResponse("Track 1", "https://spotify.com/track1"),
                        new TopTrackResponse("Track 2", "https://spotify.com/track2")
                ),
                List.of(
                        new AlbumResponse("Album 1", "2024-01-01", "album", "https://example.com/album1.jpg", "https://spotify.com/album1"),
                        new AlbumResponse("Album 2", "2024-02-01", "single", "https://example.com/album2.jpg", "https://spotify.com/album2")
                ),
                10
        );
    }

    // =========================
    // getCached / save 테스트
    // =========================

    @Test
    @DisplayName("캐시 HIT - Redis에서 데이터를 성공적으로 조회")
    void getCached_whenCacheExists_shouldReturnCachedData() {
        // given
        given(objectValueOps.get(CACHE_KEY)).willReturn(testCacheData);

        // when
        SpotifyArtistDetailCache result = spotifyCacheService.getCached(SPOTIFY_ARTIST_ID);

        // then
        assertThat(result).isNotNull();
        assertThat(result.artistName()).isEqualTo("Test Artist");
        assertThat(result.profileImageUrl()).isEqualTo("https://example.com/image.jpg");
        assertThat(result.popularity()).isEqualTo(85.5);
        assertThat(result.topTracks()).hasSize(2);
        assertThat(result.albums()).hasSize(2);
        assertThat(result.totalAlbums()).isEqualTo(10);

        verify(objectValueOps).get(CACHE_KEY);
        verifyNoInteractions(objectMapper);
    }

    @Test
    @DisplayName("캐시 MISS - Redis에 데이터가 없을 때 null 반환")
    void getCached_whenCacheNotExists_shouldReturnNull() {
        // given
        given(objectValueOps.get(CACHE_KEY)).willReturn(null);

        // when
        SpotifyArtistDetailCache result = spotifyCacheService.getCached(SPOTIFY_ARTIST_ID);

        // then
        assertThat(result).isNull();
        verify(objectValueOps).get(CACHE_KEY);
        verifyNoInteractions(objectMapper);
    }

    @Test
    @DisplayName("Redis 조회 실패 시 예외 처리 - null 반환")
    void getCached_whenRedisFails_shouldReturnNull() {
        // given
        given(objectValueOps.get(CACHE_KEY))
                .willThrow(new RuntimeException("Redis connection failed"));

        // when
        SpotifyArtistDetailCache result = spotifyCacheService.getCached(SPOTIFY_ARTIST_ID);

        // then
        assertThat(result).isNull();
        verify(objectValueOps).get(CACHE_KEY);
        verifyNoInteractions(objectMapper);
    }

    @Test
    @DisplayName("LinkedHashMap 등으로 역직렬화된 경우 ObjectMapper로 변환")
    void getCached_whenLinkedHashMap_shouldConvertWithObjectMapper() {
        // given
        LinkedHashMap<String, Object> linkedHashMap = new LinkedHashMap<>();
        linkedHashMap.put("artistName", "Test Artist");
        linkedHashMap.put("profileImageUrl", "https://example.com/image.jpg");
        linkedHashMap.put("popularity", 85.5);
        linkedHashMap.put("topTracks", List.of());
        linkedHashMap.put("albums", List.of());
        linkedHashMap.put("totalAlbums", 10);

        given(objectValueOps.get(CACHE_KEY)).willReturn(linkedHashMap);
        given(objectMapper.convertValue(linkedHashMap, SpotifyArtistDetailCache.class))
                .willReturn(testCacheData);

        // when
        SpotifyArtistDetailCache result = spotifyCacheService.getCached(SPOTIFY_ARTIST_ID);

        // then
        assertThat(result).isEqualTo(testCacheData);
        verify(objectValueOps).get(CACHE_KEY);
        verify(objectMapper).convertValue(linkedHashMap, SpotifyArtistDetailCache.class);
    }

    @Test
    @DisplayName("캐시 저장 - Redis에 데이터를 성공적으로 저장")
    void save_shouldStoreDataInRedis() {
        // given
        willDoNothing().given(objectValueOps).set(anyString(), any(), anyLong(), any(TimeUnit.class));

        // when
        spotifyCacheService.save(SPOTIFY_ARTIST_ID, testCacheData);

        // then
        verify(objectValueOps).set(
                eq(CACHE_KEY),
                eq(testCacheData),
                eq(CACHE_TTL_SECONDS),
                eq(TimeUnit.SECONDS)
        );
    }

    @Test
    @DisplayName("캐시 저장 실패 시 예외 처리 - 로그만 남기고 계속 진행")
    void save_whenRedisFails_shouldLogAndContinue() {
        // given
        willThrow(new RuntimeException("Redis connection failed"))
                .given(objectValueOps).set(anyString(), any(), anyLong(), any(TimeUnit.class));

        // when & then
        assertThatCode(() -> spotifyCacheService.save(SPOTIFY_ARTIST_ID, testCacheData))
                .doesNotThrowAnyException();

        verify(objectValueOps).set(eq(CACHE_KEY), eq(testCacheData), eq(CACHE_TTL_SECONDS), eq(TimeUnit.SECONDS));
    }

    // =========================
    // getOrFetchWithLock 테스트
    // (락 템플릿 스텁은 각 테스트에서만 추가)
    // =========================

    @Test
    @DisplayName("락 획득 성공 시 API 호출 후 캐시 저장")
    void getOrFetchWithLock_whenLockAcquired_shouldCallApiAndSaveCache() {
        // given
        when(redisTemplate.opsForValue()).thenReturn(stringValueOps);
        given(stringValueOps.setIfAbsent(eq(LOCK_KEY), eq("locked"), eq(LOCK_TTL_SECONDS), eq(TimeUnit.SECONDS)))
                .willReturn(true);

        given(objectValueOps.get(CACHE_KEY)).willReturn(null); // double-check MISS
        willDoNothing().given(objectValueOps).set(anyString(), any(), anyLong(), any(TimeUnit.class));
        given(redisTemplate.delete(LOCK_KEY)).willReturn(true);

        // when
        SpotifyArtistDetailCache result = spotifyCacheService.getOrFetchWithLock(SPOTIFY_ARTIST_ID, () -> testCacheData);

        // then
        assertThat(result).isEqualTo(testCacheData);
        verify(stringValueOps).setIfAbsent(eq(LOCK_KEY), eq("locked"), eq(LOCK_TTL_SECONDS), eq(TimeUnit.SECONDS));
        verify(objectValueOps, atLeastOnce()).get(CACHE_KEY);
        verify(objectValueOps).set(eq(CACHE_KEY), eq(testCacheData), eq(CACHE_TTL_SECONDS), eq(TimeUnit.SECONDS));
        verify(redisTemplate).delete(LOCK_KEY);
    }

    @Test
    @DisplayName("락 획득 후 Double-check에서 캐시 HIT (API 호출 X, 저장 X)")
    void getOrFetchWithLock_whenLockAcquiredButCacheExists_shouldReturnCachedData() {
        // given
        when(redisTemplate.opsForValue()).thenReturn(stringValueOps);
        given(stringValueOps.setIfAbsent(eq(LOCK_KEY), eq("locked"), eq(LOCK_TTL_SECONDS), eq(TimeUnit.SECONDS)))
                .willReturn(true);

        given(objectValueOps.get(CACHE_KEY)).willReturn(testCacheData); // double-check HIT
        given(redisTemplate.delete(LOCK_KEY)).willReturn(true);

        // when
        SpotifyArtistDetailCache result = spotifyCacheService.getOrFetchWithLock(
                SPOTIFY_ARTIST_ID,
                () -> { throw new RuntimeException("API should not be called"); }
        );

        // then
        assertThat(result).isEqualTo(testCacheData);
        verify(objectValueOps).get(CACHE_KEY);
        verify(objectValueOps, never()).set(anyString(), any(), anyLong(), any(TimeUnit.class));
        verify(redisTemplate).delete(LOCK_KEY);
    }

    @Test
    @DisplayName("락 획득 성공인데 API 호출이 예외를 던지는 경우 - 락은 반드시 해제")
    void getOrFetchWithLock_whenLockAcquiredButApiFails_shouldReleaseLock() {
        // given
        when(redisTemplate.opsForValue()).thenReturn(stringValueOps);
        given(stringValueOps.setIfAbsent(eq(LOCK_KEY), eq("locked"), eq(LOCK_TTL_SECONDS), eq(TimeUnit.SECONDS)))
                .willReturn(true);

        given(objectValueOps.get(CACHE_KEY)).willReturn(null);
        given(redisTemplate.delete(LOCK_KEY)).willReturn(true);

        RuntimeException apiException = new RuntimeException("API call failed");

        // when
        assertThatThrownBy(() -> spotifyCacheService.getOrFetchWithLock(
                SPOTIFY_ARTIST_ID,
                () -> { throw apiException; }
        )).isSameAs(apiException);

        // then
        verify(objectValueOps).get(CACHE_KEY);
        verify(objectValueOps, never()).set(anyString(), any(), anyLong(), any(TimeUnit.class));
        verify(redisTemplate).delete(LOCK_KEY);
    }

    @Test
    @DisplayName("락 획득 실패 시 대기 후 캐시 재조회 HIT → API 호출 X")
    void getOrFetchWithLock_whenLockFailed_shouldWaitAndRetryCache() {
        // given
        when(redisTemplate.opsForValue()).thenReturn(stringValueOps);
        given(stringValueOps.setIfAbsent(eq(LOCK_KEY), eq("locked"), eq(LOCK_TTL_SECONDS), eq(TimeUnit.SECONDS)))
                .willReturn(false);

        given(objectValueOps.get(CACHE_KEY)).willReturn(testCacheData);

        // when
        SpotifyArtistDetailCache result = spotifyCacheService.getOrFetchWithLock(
                SPOTIFY_ARTIST_ID,
                () -> { throw new RuntimeException("API should not be called"); }
        );

        // then
        assertThat(result).isEqualTo(testCacheData);
        verify(objectValueOps, atLeastOnce()).get(CACHE_KEY);
        verify(redisTemplate, never()).delete(LOCK_KEY);
        verify(objectValueOps, never()).set(anyString(), any(), anyLong(), any(TimeUnit.class));
    }

    @Test
    @DisplayName("락 획득 실패 후 재시도 중 캐시 HIT → API 호출 X")
    void getOrFetchWithLock_whenLockFailedAndRetrySucceeds_shouldReturnCachedData() {
        // given
        when(redisTemplate.opsForValue()).thenReturn(stringValueOps);
        given(stringValueOps.setIfAbsent(eq(LOCK_KEY), eq("locked"), eq(LOCK_TTL_SECONDS), eq(TimeUnit.SECONDS)))
                .willReturn(false);

        given(objectValueOps.get(CACHE_KEY))
                .willReturn(null)
                .willReturn(testCacheData);

        // when
        SpotifyArtistDetailCache result = spotifyCacheService.getOrFetchWithLock(
                SPOTIFY_ARTIST_ID,
                () -> { throw new RuntimeException("API should not be called"); }
        );

        // then
        assertThat(result).isEqualTo(testCacheData);
        verify(objectValueOps, atLeast(2)).get(CACHE_KEY);
        verify(objectValueOps, never()).set(anyString(), any(), anyLong(), any(TimeUnit.class));
    }

    @Test
    @DisplayName("락 획득 실패 후 최종적으로도 캐시 없으면 직접 API 호출 후 캐시 저장")
    void getOrFetchWithLock_whenLockFailedAndNoCache_shouldCallApiDirectly() {
        // given
        when(redisTemplate.opsForValue()).thenReturn(stringValueOps);
        given(stringValueOps.setIfAbsent(eq(LOCK_KEY), eq("locked"), eq(LOCK_TTL_SECONDS), eq(TimeUnit.SECONDS)))
                .willReturn(false);

        given(objectValueOps.get(CACHE_KEY)).willReturn(null);
        willDoNothing().given(objectValueOps).set(anyString(), any(), anyLong(), any(TimeUnit.class));

        AtomicInteger apiCalls = new AtomicInteger(0);

        // when
        SpotifyArtistDetailCache result = spotifyCacheService.getOrFetchWithLock(
                SPOTIFY_ARTIST_ID,
                () -> {
                    apiCalls.incrementAndGet();
                    return testCacheData;
                }
        );

        // then
        assertThat(result).isEqualTo(testCacheData);
        assertThat(apiCalls.get()).isEqualTo(1);

        verify(objectValueOps, atLeast(2)).get(CACHE_KEY);
        verify(objectValueOps).set(eq(CACHE_KEY), eq(testCacheData), eq(CACHE_TTL_SECONDS), eq(TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("setIfAbsent()가 Redis 예외를 던지면 fallback으로 API 호출 + 캐시 저장")
    void getOrFetchWithLock_whenSetIfAbsentThrows_shouldFallbackToApiAndCache() {
        // given
        when(redisTemplate.opsForValue()).thenReturn(stringValueOps);
        given(stringValueOps.setIfAbsent(eq(LOCK_KEY), eq("locked"), eq(LOCK_TTL_SECONDS), eq(TimeUnit.SECONDS)))
                .willThrow(new RuntimeException("Redis down"));

        willDoNothing().given(objectValueOps).set(anyString(), any(), anyLong(), any(TimeUnit.class));

        AtomicInteger apiCalls = new AtomicInteger(0);

        // when
        SpotifyArtistDetailCache result = spotifyCacheService.getOrFetchWithLock(
                SPOTIFY_ARTIST_ID,
                () -> {
                    apiCalls.incrementAndGet();
                    return testCacheData;
                }
        );

        // then
        assertThat(result).isEqualTo(testCacheData);
        assertThat(apiCalls.get()).isEqualTo(1);

        verify(objectValueOps).set(eq(CACHE_KEY), eq(testCacheData), eq(CACHE_TTL_SECONDS), eq(TimeUnit.SECONDS));
        verify(redisTemplate, never()).delete(LOCK_KEY);
    }

    @Test
    @DisplayName("동시 요청 시 캐시 스탬피드 방지 - 정확히 1번만 API 호출")
    void getOrFetchWithLock_whenConcurrentRequests_shouldCallApiExactlyOnce() throws InterruptedException {
        // given
        when(redisTemplate.opsForValue()).thenReturn(stringValueOps);

        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);

        AtomicInteger apiCallCount = new AtomicInteger(0);
        AtomicInteger lockAttemptCount = new AtomicInteger(0);
        AtomicBoolean cached = new AtomicBoolean(false);

        given(stringValueOps.setIfAbsent(eq(LOCK_KEY), eq("locked"), eq(LOCK_TTL_SECONDS), eq(TimeUnit.SECONDS)))
                .willAnswer(inv -> lockAttemptCount.getAndIncrement() == 0);

        given(objectValueOps.get(CACHE_KEY)).willAnswer(inv -> cached.get() ? testCacheData : null);

        willAnswer(inv -> {
            cached.set(true);
            return null;
        }).given(objectValueOps).set(eq(CACHE_KEY), any(), eq(CACHE_TTL_SECONDS), eq(TimeUnit.SECONDS));

        given(redisTemplate.delete(LOCK_KEY)).willReturn(true);

        // when
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    spotifyCacheService.getOrFetchWithLock(
                            SPOTIFY_ARTIST_ID,
                            () -> {
                                apiCallCount.incrementAndGet();
                                // 테스트 안정성: supplier 진입 시 cached=true로 올려도 됨
                                cached.set(true);
                                return testCacheData;
                            }
                    );
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        endLatch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        // then
        assertThat(apiCallCount.get()).isEqualTo(1);
    }
}
