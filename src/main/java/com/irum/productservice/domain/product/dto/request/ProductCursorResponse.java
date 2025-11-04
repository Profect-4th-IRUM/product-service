package com.irum.productservice.domain.product.dto.request;

import com.irum.productservice.domain.product.dto.response.ProductResponse;
import java.util.List;
import java.util.UUID;

public record ProductCursorResponse(List<ProductResponse> products, UUID nextCursor) {
    public static ProductCursorResponse of(List<ProductResponse> products) {
        UUID nextCursor = products.isEmpty() ? null : products.get(products.size() - 1).id();
        return new ProductCursorResponse(products, nextCursor);
    }
}
