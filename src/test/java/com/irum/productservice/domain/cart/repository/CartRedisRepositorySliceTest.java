package com.irum.productservice.domain.cart.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.irum.productservice.domain.cart.domain.entity.CartRedis;
import com.irum.productservice.domain.cart.domain.repository.CartRedisRepository;
import com.irum.productservice.testsupport.EmbeddedRedisConfig;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.redis.DataRedisTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;

@DataRedisTest
@Import(EmbeddedRedisConfig.class)
class CartRedisRepositorySliceTest {

    @Autowired private CartRedisRepository repository;
    @Autowired private StringRedisTemplate stringRedisTemplate;

    @AfterAll
    static void shutdown() {
        EmbeddedRedisConfig.shutdown();
    }

    @Test
    void 저장하고_TTL_만료되면_사라진다() throws Exception {
        UUID cartId = UUID.randomUUID();
        UUID optionValueId = UUID.randomUUID();

        var saved =
                repository.save(
                        CartRedis.of(1L, cartId, optionValueId, 2, 2L) // TTL=2초
                        );
        String key = "cart:" + saved.getId();

        Long ttl = stringRedisTemplate.getExpire(key, TimeUnit.SECONDS);
        assertThat(ttl).isGreaterThan(0);

        Thread.sleep(2500); // 2초 + 여유
        Optional<CartRedis> after = repository.findById(saved.getId());
        assertThat(after).isEmpty();
        assertThat(stringRedisTemplate.hasKey(key)).isFalse();
    }
}
