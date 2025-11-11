package com.irum.productservice.domain.cart.domain.model;

import java.io.Serializable;
import java.util.UUID;
import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartRedis implements Serializable {
    private UUID cartId;
    private Long memberId;
    private UUID optionValueId;
    private int quantity;

    public void updateQuantity(int quantity) {
        this.quantity = quantity;
    }
}
