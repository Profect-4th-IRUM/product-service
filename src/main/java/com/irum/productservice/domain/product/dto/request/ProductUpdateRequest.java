package com.irum.productservice.domain.product.presentation.dto.request;

public record ProductUpdateRequest(
        String name,
        String description,
        String detailDescription,
        Boolean isPublic,
        Integer price) {}
