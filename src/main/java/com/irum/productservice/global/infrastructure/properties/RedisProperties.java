package com.irum.productservice.global.infrastructure.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "spring.data.redis")
public record RedisProperties(String host, int port, String password) {}
