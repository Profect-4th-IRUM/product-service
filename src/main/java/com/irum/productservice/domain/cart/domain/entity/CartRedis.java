package com.irum.productservice.domain.cart.domain.entity;

import java.io.Serializable;
import java.util.UUID;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;

@RedisHash("cart")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartRedis implements Serializable {

    @Id private String id; // Redis key (cart:<uuid>)

    private Long memberId;
    private UUID optionValueId;
    private UUID cartId;
    private Integer quantity;

    @TimeToLive private Long ttlSeconds;

    public static CartRedis of(
            Long memberId, UUID cartId, UUID optionValueId, int quantity, long ttlSeconds) {
        return CartRedis.builder()
                .id(UUID.randomUUID().toString())
                .memberId(memberId)
                .cartId(cartId)
                .optionValueId(optionValueId)
                .quantity(quantity)
                .ttlSeconds(ttlSeconds)
                .build();
    }

    public void updateQuantity(int q) {
        this.quantity = q;
    }

    public void refreshTtl(long ttlSeconds) {
        this.ttlSeconds = ttlSeconds;
    }
}
