package com.irum.productservice.domain.cart.domain.entity;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;
import org.springframework.data.redis.core.index.Indexed;

@RedisHash("cart")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartRedis implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    private String id;

    @Indexed
    private Long memberId;

    @Indexed
    private UUID optionValueId;

    @Indexed
    private UUID cartId;

    private Integer quantity;

    @TimeToLive
    private Long ttlSeconds;

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
}
