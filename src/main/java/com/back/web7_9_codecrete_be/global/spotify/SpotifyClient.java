package com.back.web7_9_codecrete_be.global.spotify;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.model_objects.credentials.ClientCredentials;
import se.michaelthelin.spotify.requests.authorization.client_credentials.ClientCredentialsRequest;

import java.util.concurrent.locks.ReentrantLock;

/**
 * Spotify AccessToken 중앙 관리 클래스
 * 
 * - 토큰 만료 시간 관리 (1시간 유효)
 * - 만료 전 자동 재발급 (5분 여유)
 * - 401 에러 발생 시 자동 재발급 및 재요청 지원
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SpotifyClient {

    private final SpotifyApi spotifyApi;
    
    // 토큰 관리 필드
    private volatile String cachedAccessToken;
    private volatile long tokenExpiresAt = 0; // 만료 시각 (밀리초)
    private final ReentrantLock tokenLock = new ReentrantLock();
    
    // 토큰 만료 여유 시간 (5분 = 300초)
    private static final long TOKEN_BUFFER_SECONDS = 300;
    private static final long TOKEN_BUFFER_MS = TOKEN_BUFFER_SECONDS * 1000;
    
    /**
     * AccessToken 발급 (내부 메서드)
     * 동시성 제어를 통해 중복 발급 방지
     */
    private String refreshAccessToken() {
        tokenLock.lock();
        try {
            // Double-check: 다른 스레드가 이미 토큰을 발급했는지 확인
            if (cachedAccessToken != null && System.currentTimeMillis() < tokenExpiresAt) {
                return cachedAccessToken;
            }
            
            log.info("Spotify AccessToken 발급 시작");
            ClientCredentialsRequest request = spotifyApi.clientCredentials().build();
            ClientCredentials credentials = request.execute();
            
            String newToken = credentials.getAccessToken();
            // Spotify 토큰은 3600초(1시간) 유효
            // 여유 시간을 빼서 실제 만료 시각 계산
            long expiresInSeconds = credentials.getExpiresIn();
            tokenExpiresAt = System.currentTimeMillis() + (expiresInSeconds * 1000) - TOKEN_BUFFER_MS;
            
            cachedAccessToken = newToken;
            spotifyApi.setAccessToken(newToken);
            
            log.info("Spotify AccessToken 발급 완료. 만료 시각: {} ({}초 후)", 
                    new java.util.Date(tokenExpiresAt), expiresInSeconds);
            
            return newToken;
        } catch (Exception e) {
            log.error("Spotify 토큰 발급 실패", e);
            // 토큰 발급 실패 시 캐시 초기화
            cachedAccessToken = null;
            tokenExpiresAt = 0;
            throw new RuntimeException("Spotify 토큰 발급 실패", e);
        } finally {
            tokenLock.unlock();
        }
    }
    
    /**
     * AccessToken 조회 (만료 체크 포함)
     * 만료되었거나 곧 만료될 경우 자동 재발급
     */
    public String getAccessToken() {
        long now = System.currentTimeMillis();
        
        // 토큰이 없거나 만료되었거나 곧 만료될 경우 재발급
        if (cachedAccessToken == null || now >= tokenExpiresAt) {
            return refreshAccessToken();
        }
        
        return cachedAccessToken;
    }
    
    /**
     * 인증된 SpotifyApi 반환
     * 토큰이 없거나 만료된 경우 자동으로 재발급
     */
    public SpotifyApi getAuthorizedApi() {
        getAccessToken(); // 만료 체크 및 필요시 재발급
        return spotifyApi;
    }
    
    /**
     * 401 에러 발생 시 토큰 강제 재발급
     * 이 메서드를 호출한 후 API를 재요청해야 함
     */
    public void forceRefreshToken() {
        log.warn("401 에러 감지 - Spotify AccessToken 강제 재발급");
        tokenLock.lock();
        try {
            // 캐시 초기화 후 재발급
            cachedAccessToken = null;
            tokenExpiresAt = 0;
            refreshAccessToken();
        } finally {
            tokenLock.unlock();
        }
    }
    
    /**
     * 토큰 만료 여부 확인
     */
    public boolean isTokenExpired() {
        return cachedAccessToken == null || System.currentTimeMillis() >= tokenExpiresAt;
    }
}
