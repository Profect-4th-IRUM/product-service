package com.irum.productservice.domain.cart.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.irum.productservice.domain.cart.domain.entity.CartItem;
import com.irum.productservice.domain.cart.domain.repository.CartItemRepository;
import com.irum.productservice.testsupport.EmbeddedRedisConfig;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.redis.DataRedisTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

@DataRedisTest
@ActiveProfiles("test")
@Import(EmbeddedRedisConfig.class)
class CartItemRepositorySliceTest {

    @Autowired private CartItemRepository repository;

    @Autowired private StringRedisTemplate stringRedisTemplate;

    @Test
    void 저장하고_TTL_만료되면_사라진다() throws Exception {
        // given
        UUID optionValueId = UUID.randomUUID();

        CartItem saved =
                repository.save(
                        CartItem.of(1L, optionValueId, 2, 2L) // TTL = 2초
                        );
        String key = "cart:" + saved.getCartItemId();

        // when & then
        Long ttl = stringRedisTemplate.getExpire(key, TimeUnit.SECONDS);
        assertThat(ttl).isGreaterThan(0);

        Thread.sleep(2500); // 2초 + 여유

        Optional<CartItem> after = repository.findById(saved.getCartItemId());
        assertThat(after).isEmpty();
        assertThat(stringRedisTemplate.hasKey(key)).isFalse();
    }
}
