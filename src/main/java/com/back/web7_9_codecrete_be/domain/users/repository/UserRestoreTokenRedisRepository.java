package com.back.web7_9_codecrete_be.domain.users.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.concurrent.TimeUnit;

@Repository
@RequiredArgsConstructor
public class UserRestoreTokenRedisRepository {

    private final StringRedisTemplate redisTemplate;
    private static final String PREFIX = "user_restore:";
    private static final long TTL_MINUTES = 15;

    public void save(String token, String email) {
        redisTemplate.opsForValue()
                .set(PREFIX + token, email, TTL_MINUTES, TimeUnit.MINUTES);
    }

    public String findEmailByToken(String token) {
        return redisTemplate.opsForValue().get(PREFIX + token);
    }

    public void delete(String token) {
        redisTemplate.delete(PREFIX + token);
    }
}
