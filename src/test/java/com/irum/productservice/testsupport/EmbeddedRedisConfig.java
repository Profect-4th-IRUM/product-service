package com.irum.productservice.testsupport;

import java.io.IOException;
import java.net.ServerSocket;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import redis.embedded.RedisServer;

/**
 * 테스트에서 사용할 임베디드 Redis 서버를 띄우고 spring.data.redis.host/port 속성을 동적으로 주입한다.
 *
 * <p>사용법: @Import(EmbeddedRedisConfig.class) public class XxxTest { ... }
 */
public abstract class EmbeddedRedisConfig {

    private static RedisServer redisServer;
    private static int port;

    static {
        try {
            port = findFreePort();
            redisServer = new RedisServer(port);
            redisServer.start();
        } catch (IOException e) {
            throw new RuntimeException("Failed to start embedded Redis", e);
        }
    }

    @DynamicPropertySource
    static void registerRedisProps(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", () -> "127.0.0.1");
        registry.add("spring.data.redis.port", () -> port);
    }

    private static int findFreePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            socket.setReuseAddress(true);
            return socket.getLocalPort();
        }
    }

    public static void shutdown() {
        if (redisServer != null && redisServer.isActive()) {
            redisServer.stop();
        }
    }
}
