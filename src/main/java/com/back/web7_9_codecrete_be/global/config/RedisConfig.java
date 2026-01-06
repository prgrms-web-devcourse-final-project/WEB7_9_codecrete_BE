package com.back.web7_9_codecrete_be.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class RedisConfig {
    @Value("${spring.data.redis.host}")
    private String host;

    @Value("${spring.data.redis.port}")
    private int port;

    //Redis와 연결을 위한 Connection 생성
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        final RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration(host,port);
        return new LettuceConnectionFactory(configuration);
    }

    //문자열만 사용할 때(refreshToken)
    @Bean
    public StringRedisTemplate stringRedisTemplate() {
        return new StringRedisTemplate(redisConnectionFactory());
    }

    //Json 직렬화 필요할 때(복잡한 객체 저장 등)
    @Bean
    public RedisTemplate<String, Object> redisTemplate(ObjectMapper objectMapper) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();

        GenericJackson2JsonRedisSerializer serializer =
            new GenericJackson2JsonRedisSerializer(objectMapper);

        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);

        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(serializer);

        template.setConnectionFactory(redisConnectionFactory());
        return template;
    }


    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory cf) {

        RedisCacheConfiguration redisCacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30));      // TTL 30분

        Map<String, RedisCacheConfiguration> configs = new HashMap<>();     // 캐시 이름마다 다른 TTL 설정
        configs.put("nearByCafes", RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofMinutes(30)));
        configs.put("nearByRestaurants", RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofMinutes(30)));

        return RedisCacheManager.builder(cf)
                .cacheDefaults(redisCacheConfig)
                .withInitialCacheConfigurations(configs)
                .build();
    }
}
