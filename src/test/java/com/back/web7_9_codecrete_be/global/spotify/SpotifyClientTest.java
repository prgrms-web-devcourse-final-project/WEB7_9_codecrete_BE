package com.back.web7_9_codecrete_be.global.spotify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.model_objects.credentials.ClientCredentials;
import se.michaelthelin.spotify.requests.authorization.client_credentials.ClientCredentialsRequest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SpotifyClient 토큰 관리 테스트")
class SpotifyClientTest {

    @Mock
    private SpotifyApi spotifyApi;

    @Mock
    private ClientCredentialsRequest.Builder requestBuilder;

    @Mock
    private ClientCredentialsRequest request;

    @InjectMocks
    private SpotifyClient spotifyClient;

    @BeforeEach
    void setUp() {
        // ReflectionTestUtils로 private 필드 초기화
        ReflectionTestUtils.setField(spotifyClient, "cachedAccessToken", null);
        ReflectionTestUtils.setField(spotifyClient, "tokenExpiresAt", 0L);
    }

    @Test
    @DisplayName("토큰이 없을 때 자동 발급")
    void getAccessToken_whenTokenIsNull_shouldIssueNewToken() throws Exception {
        // given
        String expectedToken = "new-access-token-123";
        ClientCredentials credentials = mock(ClientCredentials.class);
        
        given(spotifyApi.clientCredentials()).willReturn(requestBuilder);
        given(requestBuilder.build()).willReturn(request);
        given(request.execute()).willReturn(credentials);
        given(credentials.getAccessToken()).willReturn(expectedToken);
        given(credentials.getExpiresIn()).willReturn(3600); // 1시간

        // when
        String token = spotifyClient.getAccessToken();

        // then
        assertThat(token).isEqualTo(expectedToken);
        verify(spotifyApi, times(1)).setAccessToken(expectedToken);
    }

    @Test
    @DisplayName("토큰이 만료되었을 때 자동 재발급")
    void getAccessToken_whenTokenExpired_shouldRefreshToken() throws Exception {
        // given
        String oldToken = "old-token";
        String newToken = "new-token-456";
        long expiredTime = System.currentTimeMillis() - 1000; // 1초 전에 만료
        
        ReflectionTestUtils.setField(spotifyClient, "cachedAccessToken", oldToken);
        ReflectionTestUtils.setField(spotifyClient, "tokenExpiresAt", expiredTime);

        ClientCredentials credentials = mock(ClientCredentials.class);
        given(spotifyApi.clientCredentials()).willReturn(requestBuilder);
        given(requestBuilder.build()).willReturn(request);
        given(request.execute()).willReturn(credentials);
        given(credentials.getAccessToken()).willReturn(newToken);
        given(credentials.getExpiresIn()).willReturn(3600);

        // when
        String token = spotifyClient.getAccessToken();

        // then
        assertThat(token).isEqualTo(newToken);
        assertThat(token).isNotEqualTo(oldToken);
        verify(spotifyApi, times(1)).setAccessToken(newToken);
    }

    @Test
    @DisplayName("토큰이 유효할 때 재사용")
    void getAccessToken_whenTokenValid_shouldReuseToken() throws Exception {
        // given
        String validToken = "valid-token-789";
        long futureExpiry = System.currentTimeMillis() + 1000000; // 미래에 만료
        
        ReflectionTestUtils.setField(spotifyClient, "cachedAccessToken", validToken);
        ReflectionTestUtils.setField(spotifyClient, "tokenExpiresAt", futureExpiry);

        // when
        String token = spotifyClient.getAccessToken();

        // then
        assertThat(token).isEqualTo(validToken);
        // 토큰이 유효하면 재발급하지 않아야 함
        verify(spotifyApi, never()).clientCredentials();
    }

    @Test
    @DisplayName("forceRefreshToken은 토큰을 강제로 재발급")
    void forceRefreshToken_shouldForceRefresh() throws Exception {
        // given
        String oldToken = "old-token";
        String newToken = "forced-new-token";
        long futureExpiry = System.currentTimeMillis() + 1000000; // 아직 유효한 토큰
        
        ReflectionTestUtils.setField(spotifyClient, "cachedAccessToken", oldToken);
        ReflectionTestUtils.setField(spotifyClient, "tokenExpiresAt", futureExpiry);

        ClientCredentials credentials = mock(ClientCredentials.class);
        given(spotifyApi.clientCredentials()).willReturn(requestBuilder);
        given(requestBuilder.build()).willReturn(request);
        given(request.execute()).willReturn(credentials);
        given(credentials.getAccessToken()).willReturn(newToken);
        given(credentials.getExpiresIn()).willReturn(3600);

        // when
        spotifyClient.forceRefreshToken();
        String token = spotifyClient.getAccessToken();

        // then
        assertThat(token).isEqualTo(newToken);
        assertThat(token).isNotEqualTo(oldToken);
        verify(spotifyApi, times(1)).setAccessToken(newToken);
    }

    @Test
    @DisplayName("동시에 여러 스레드가 호출해도 토큰은 한 번만 발급")
    void getAccessToken_concurrentCalls_shouldIssueTokenOnce() throws Exception {
        // given
        String expectedToken = "concurrent-token";
        ClientCredentials credentials = mock(ClientCredentials.class);
        
        given(spotifyApi.clientCredentials()).willReturn(requestBuilder);
        given(requestBuilder.build()).willReturn(request);
        given(request.execute()).willReturn(credentials);
        given(credentials.getAccessToken()).willReturn(expectedToken);
        given(credentials.getExpiresIn()).willReturn(3600);

        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger tokenIssueCount = new AtomicInteger(0);

        // when
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    spotifyClient.getAccessToken();
                    tokenIssueCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        // then
        // 모든 스레드가 동일한 토큰을 받아야 함
        assertThat(tokenIssueCount.get()).isEqualTo(threadCount);
        // 실제 토큰 발급은 한 번만 일어나야 함 (동시성 제어)
        verify(spotifyApi, atMostOnce()).clientCredentials();
    }

    @Test
    @DisplayName("토큰 만료 여부 확인")
    void isTokenExpired_shouldReturnCorrectStatus() {
        // given - 만료된 토큰
        ReflectionTestUtils.setField(spotifyClient, "cachedAccessToken", "expired-token");
        ReflectionTestUtils.setField(spotifyClient, "tokenExpiresAt", System.currentTimeMillis() - 1000);

        // when & then
        assertThat(spotifyClient.isTokenExpired()).isTrue();

        // given - 유효한 토큰
        ReflectionTestUtils.setField(spotifyClient, "tokenExpiresAt", System.currentTimeMillis() + 1000000);

        // when & then
        assertThat(spotifyClient.isTokenExpired()).isFalse();

        // given - 토큰이 null
        ReflectionTestUtils.setField(spotifyClient, "cachedAccessToken", null);

        // when & then
        assertThat(spotifyClient.isTokenExpired()).isTrue();
    }

    @Test
    @DisplayName("getAuthorizedApi는 유효한 토큰이 있는 API를 반환")
    void getAuthorizedApi_shouldReturnApiWithValidToken() throws Exception {
        // given
        String expectedToken = "api-token";
        ClientCredentials credentials = mock(ClientCredentials.class);
        
        given(spotifyApi.clientCredentials()).willReturn(requestBuilder);
        given(requestBuilder.build()).willReturn(request);
        given(request.execute()).willReturn(credentials);
        given(credentials.getAccessToken()).willReturn(expectedToken);
        given(credentials.getExpiresIn()).willReturn(3600);

        // when
        SpotifyApi api = spotifyClient.getAuthorizedApi();

        // then
        assertThat(api).isNotNull();
        assertThat(api).isEqualTo(spotifyApi);
        verify(spotifyApi, times(1)).setAccessToken(expectedToken);
    }
}

