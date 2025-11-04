package com.irum.productservice.domain.product.dto.request;

import jakarta.validation.constraints.Min;

public record ProductOptionValueUpdateRequest(
        String name,
        @Min(value = 0, message = "재고 수량은 0 이상이어야 합니다.") Integer stockQuantity,
        Integer extraPrice) {}
