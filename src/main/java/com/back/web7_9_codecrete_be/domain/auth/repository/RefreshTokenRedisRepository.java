package com.back.web7_9_codecrete_be.domain.auth.repository;

import com.back.web7_9_codecrete_be.domain.auth.entity.RefreshToken;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.concurrent.TimeUnit;

@Repository
@RequiredArgsConstructor
public class RefreshTokenRedisRepository {
    private final RedisTemplate<String, String> redisTemplate;

    private static final String REFRESH_TOKEN_PREFIX = "refreshToken: ";

    public void save(RefreshToken refreshToken) {
        redisTemplate.opsForValue().set(
                REFRESH_TOKEN_PREFIX + refreshToken.getUserId(),
                refreshToken.getRefreshToken(),
                refreshToken.getExpiration(),
                TimeUnit.SECONDS
        );
    }

    public String findByUserId(Long userId) {
        return (String) redisTemplate.opsForValue().get(REFRESH_TOKEN_PREFIX + userId);
    }

    public void deleteByUserId(Long userId){
        redisTemplate.delete(REFRESH_TOKEN_PREFIX + userId);
    }

    public boolean existsByUserId(Long userId) {
        return redisTemplate.hasKey(REFRESH_TOKEN_PREFIX + userId);
    }
}
