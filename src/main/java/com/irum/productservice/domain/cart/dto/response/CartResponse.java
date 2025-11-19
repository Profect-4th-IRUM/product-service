package com.irum.productservice.domain.cart.dto.response;

import java.util.UUID;
import lombok.Builder;

@Builder
public record CartResponse(
        String cartItemId,
        UUID optionValueId,
        String productName,
        String optionValueName,
        String imageUrl,
        int quantity,
        int basePrice, // 상품 기본가
        int extraPrice, // 옵션 추가금
        int discountAmount, // 할인 금액
        int unitPrice, // 단가 (base + extra - discount)
        int lineTotal, // 총액 (unitPrice * quantity)
        int stockQuantity // 옵션 재고 수량
        ) {}
