package com.back.web7_9_codecrete_be.domain.concerts.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.concurrent.TimeUnit;

@Repository
@RequiredArgsConstructor
public class ConcertRedisRepository {
    private final RedisTemplate<String,String> redisTemplate;

    private static final String LOCK_FLAG_PREFIX = "initLoad: ";

    public void lockSave(String key, String value) {
        redisTemplate.opsForValue().set(
                LOCK_FLAG_PREFIX + key,
                value,
                900,
                TimeUnit.SECONDS);
    }

    public String lockGet(String key) {
        return redisTemplate.opsForValue().get(LOCK_FLAG_PREFIX + key);
    }

    public void unlockSave(String key) {
        redisTemplate.delete(LOCK_FLAG_PREFIX + key);
    }

}
