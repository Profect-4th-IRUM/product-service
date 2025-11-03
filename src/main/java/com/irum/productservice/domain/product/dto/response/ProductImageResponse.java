package com.irum.productservice.domain.product.presentation.dto.response;

import com.irum.productservice.domain.product.domain.entity.ProductImage;
import java.util.UUID;

public record ProductImageResponse(UUID id, String imageUrl, boolean isDefault) {
    public static ProductImageResponse from(ProductImage productImage) {
        return new ProductImageResponse(
                productImage.getId(), productImage.getImageUrl(), productImage.isDefault());
    }
}
