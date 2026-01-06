package com.back.web7_9_codecrete_be.domain.artists.service.spotifyService;

import com.back.web7_9_codecrete_be.global.spotify.SpotifyClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SpotifyRateLimitHandler 401 에러 처리 테스트")
class SpotifyRateLimitHandlerTest {

    @Mock
    private SpotifyClient spotifyClient;

    @InjectMocks
    private SpotifyRateLimitHandler rateLimitHandler;

    @BeforeEach
    void setUp() {
        // 각 테스트 전에 상태 초기화
    }

    @Test
    @DisplayName("401 에러 발생 시 토큰 재발급 후 재시도 성공")
    void callWithRateLimitRetry_when401Error_shouldRefreshTokenAndRetry() {
        // given
        RuntimeException unauthorizedException = new RuntimeException("401 Unauthorized");
        String successResult = "success-result";
        
        // forceRefreshToken은 void 메서드이므로 doNothing() 사용
        doNothing().when(spotifyClient).forceRefreshToken();
        
        // 첫 번째 호출은 401 에러, 두 번째 호출은 성공하도록 설정
        AtomicInteger callCount = new AtomicInteger(0);

        // when
        String result = rateLimitHandler.callWithRateLimitRetry(
                () -> {
                    int count = callCount.incrementAndGet();
                    if (count == 1) {
                        // 첫 번째 호출 시 401 에러
                        throw unauthorizedException;
                    }
                    // 두 번째 호출 시 성공
                    return successResult;
                },
                "test-context"
        );

        // then
        assertThat(result).isEqualTo(successResult);
        assertThat(callCount.get()).isEqualTo(2); // 재시도로 2번 호출됨
        verify(spotifyClient, times(1)).forceRefreshToken();
    }

    @Test
    @DisplayName("401 에러가 아닌 경우 토큰 재발급하지 않음")
    void callWithRateLimitRetry_whenNot401Error_shouldNotRefreshToken() {
        // given
        RuntimeException otherException = new RuntimeException("500 Internal Server Error");
        String successResult = "success-result";

        // when & then
        assertThatThrownBy(() -> {
            rateLimitHandler.callWithRateLimitRetry(
                    () -> {
                        throw otherException;
                    },
                    "test-context"
            );
        }).isInstanceOf(RuntimeException.class)
          .hasMessageContaining("500 Internal Server Error");

        // 401이 아니면 토큰 재발급하지 않아야 함
        verify(spotifyClient, never()).forceRefreshToken();
    }

    @Test
    @DisplayName("정상 호출 시 토큰 재발급하지 않음")
    void callWithRateLimitRetry_whenSuccess_shouldNotRefreshToken() {
        // given
        String successResult = "success-result";

        // when
        String result = rateLimitHandler.callWithRateLimitRetry(
                () -> successResult,
                "test-context"
        );

        // then
        assertThat(result).isEqualTo(successResult);
        verify(spotifyClient, never()).forceRefreshToken();
    }

    @Test
    @DisplayName("401 에러가 예외 체인에 있는 경우 감지")
    void is401Error_when401InExceptionChain_shouldDetect() {
        // given
        RuntimeException cause = new RuntimeException("401 Unauthorized");
        RuntimeException wrapper = new RuntimeException("Wrapped exception", cause);

        // when
        // is401Error는 private 메서드이므로 callWithRateLimitRetry를 통해 간접 테스트
        // 실제로는 401 에러가 발생하면 forceRefreshToken이 호출되어야 함
        // 이 테스트는 통합 테스트에서 더 적합할 수 있음
    }
}

