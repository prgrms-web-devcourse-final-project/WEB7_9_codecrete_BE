package com.back.web7_9_codecrete_be.domain.email.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.concurrent.TimeUnit;

@Repository
@RequiredArgsConstructor
public class VerificationCodeRedisRepository {

    private final RedisTemplate<String, String> redisTemplate;

    private static final String PREFIX = "email:verify:";

    public void save(String email, String code, long ttlSeconds) {
        redisTemplate.opsForValue().set(
                PREFIX + email,
                code,
                ttlSeconds,
                TimeUnit.SECONDS
        );
    }

    public String findByEmail(String email) {
        return redisTemplate.opsForValue().get(PREFIX + email);
    }

    public void deleteByEmail(String email) {
        redisTemplate.delete(PREFIX + email);
    }

    public boolean existsByEmail(String email) {
        return redisTemplate.hasKey(PREFIX + email);
    }
}
