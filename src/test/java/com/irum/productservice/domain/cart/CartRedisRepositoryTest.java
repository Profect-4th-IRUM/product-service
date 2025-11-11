package com.irum.productservice.domain.cart;

import static org.assertj.core.api.Assertions.assertThat;

import com.irum.productservice.domain.cart.domain.model.CartRedis;
import com.irum.productservice.domain.cart.domain.repository.CartRedisRepository;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

@SpringBootTest
class CartRedisRepositoryTest {

    @Autowired private CartRedisRepository cartRedisRepository;

    @Autowired private RedisTemplate<String, Object> redisTemplate;

    @Test
    void 장바구니_저장_및_TTL_확인_10초() throws Exception {
        // given
        CartRedis cart =
                CartRedis.builder().memberId(1L).cartId(UUID.randomUUID()).quantity(2).build();

        String key = "cart:" + cart.getMemberId();
        redisTemplate.opsForHash().put(key, cart.getCartId().toString(), cart);
        redisTemplate.expire(key, 10, TimeUnit.SECONDS);

        // when
        Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        System.out.println("TTL(초 단위): " + ttl);

        // then
        assertThat(ttl).isGreaterThan(0);

        // 대기 후 만료 확인
        Thread.sleep(11_000);
        Boolean exists = redisTemplate.hasKey(key);
        System.out.println("만료 후 Redis 존재 여부: " + exists);
        assertThat(exists).isFalse();
    }
}
