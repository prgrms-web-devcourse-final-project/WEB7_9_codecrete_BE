package com.back.web7_9_codecrete_be.domain.email.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.concurrent.TimeUnit;

@Repository
@RequiredArgsConstructor
public class VerifiedEmailRedisRepository {

    private final StringRedisTemplate redisTemplate;
    private static final String PREFIX = "verified_email:";
    private static final long VERIFIED_TTL_SECONDS = 60 * 30; // 30분

    public void save(String email) {
        redisTemplate.opsForValue()
                .set(PREFIX + email, "true", VERIFIED_TTL_SECONDS, TimeUnit.SECONDS);
    }

    public boolean exists(String email) {
        return Boolean.TRUE.equals(
                redisTemplate.hasKey(PREFIX + email)
        );
    }

    public void delete(String email) {
        redisTemplate.delete(PREFIX + email);
    }
}
