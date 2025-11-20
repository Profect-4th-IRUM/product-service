package com.irum.productservice.domain.cart.domain.entity;

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
public class CartItem implements Serializable {

    @Id private String cartItemId;

    @Indexed private Long memberId;

    @Indexed private UUID optionValueId;

    private Integer quantity;

    @TimeToLive private Long ttlSeconds;

    public static CartItem of(Long memberId, UUID optionValueId, int quantity, long ttlSeconds) {
        return CartItem.builder()
                .cartItemId(UUID.randomUUID().toString())
                .memberId(memberId)
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
