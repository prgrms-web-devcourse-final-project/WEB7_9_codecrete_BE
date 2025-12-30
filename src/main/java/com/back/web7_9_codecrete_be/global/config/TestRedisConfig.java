package com.back.web7_9_codecrete_be.global.config;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import redis.embedded.RedisServer;

import java.io.IOException;
import java.net.ServerSocket;

@Slf4j
@Configuration
@Profile("test")
public class TestRedisConfig {

    private int redisPort;
    private RedisServer redisServer;

    @PostConstruct
    public void startRedis() throws IOException {
        redisPort = 58091;
        redisServer = new RedisServer(redisPort);
        redisServer.start();

        System.setProperty("spring.data.redis.host","localhost");
        System.setProperty("spring.data.redis.port",String.valueOf(redisPort));
        log.info("Starting Redis on port {}",redisPort);
    }

    @PreDestroy
    public void stopRedis() throws IOException {
        if(redisServer != null && redisServer.isActive()) {
            redisServer.stop();
            log.info("Stopping Redis on port {}",redisPort);
        }
    }

    private int findFreePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            socket.setReuseAddress(true);
            return socket.getLocalPort();
        }
    }
}
