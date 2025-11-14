package com.irum.productservice.global.infrastructure.config.redis;

import com.irum.productservice.global.infrastructure.properties.RedisProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;

@Configuration
@EnableCaching
@EnableConfigurationProperties(RedisProperties.class)
@EnableRedisRepositories(basePackages = "com.irum.productservice.domain.cart.domain.repository")
@RequiredArgsConstructor
@Slf4j
public class RedisConfig {

    private final RedisProperties props;

    @PostConstruct
    public void logRedisProps() {
        log.info(
                "[RedisConfig] Redis Loaded >> host={}, port={}, password={}",
                props.host(),
                props.port(),
                props.password());
    }

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration conf =
                new RedisStandaloneConfiguration(props.host(), props.port());

        if (props.password() != null && !props.password().isBlank()) {
            conf.setPassword(RedisPassword.of(props.password()));
        }

        return new LettuceConnectionFactory(conf);
    }
}
