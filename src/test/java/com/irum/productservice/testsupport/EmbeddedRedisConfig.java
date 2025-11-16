package com.irum.productservice.testsupport;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import redis.embedded.RedisServer;

@TestConfiguration
public class EmbeddedRedisConfig {

    private static final int REDIS_PORT = 6379;
    private RedisServer redisServer;

    @PostConstruct
    public void startRedis() {
        try {
            redisServer = new RedisServer(REDIS_PORT);
            redisServer.start();
        } catch (Exception e) {
            System.err.println(
                    "Embedded Redis start failed (maybe already running?): " + e.getMessage());
        }
    }

    @PreDestroy
    public void stopRedis() {
        if (redisServer != null) {
            try {
                redisServer.stop();
            } catch (Exception e) {
                System.err.println("Embedded Redis stop failed: " + e.getMessage());
            }
        }
    }

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", () -> "127.0.0.1");
        registry.add("spring.data.redis.port", () -> REDIS_PORT);
    }
}
