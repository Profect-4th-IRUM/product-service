package com.irum.productservice.domain.product.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ProductCategoryUpdateRequest(
        @NotNull(message = "상품 카테고리는 필수 입력값입니다.") UUID categoryId) {}
