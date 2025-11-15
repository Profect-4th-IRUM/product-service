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

    @Id private String id; // Redis Key(ID)

    private Long memberId; // 멤버 ID
    private UUID optionValueId; // 옵션 값 ID
    private UUID cartId; // 장바구니 아이템 고유 ID
    private Integer quantity; // 수량

    @TimeToLive private Long ttlSeconds; // TTL

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
