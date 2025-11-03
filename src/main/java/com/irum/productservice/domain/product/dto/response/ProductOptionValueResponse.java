package com.irum.productservice.domain.product.presentation.dto.response;

import com.irum.productservice.domain.product.domain.entity.ProductOptionValue;
import java.util.UUID;

public record ProductOptionValueResponse(
        UUID id, String name, int stockQuantity, Integer extraPrice) {
    public static ProductOptionValueResponse from(ProductOptionValue value) {
        return new ProductOptionValueResponse(
                value.getId(), value.getName(), value.getStockQuantity(), value.getExtraPrice());
    }
}
