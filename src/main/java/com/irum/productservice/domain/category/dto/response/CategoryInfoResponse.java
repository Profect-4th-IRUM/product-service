package com.irum.productservice.domain.category.dto.response;

import com.irum.productservice.domain.category.domain.entity.Category;
import java.util.UUID;

public record CategoryInfoResponse(UUID categoryId, String name, int depth) {
    public static CategoryInfoResponse from(Category category) {
        return new CategoryInfoResponse(
                category.getCategoryId(), category.getName(), category.getDepth());
    }
}
