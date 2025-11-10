package com.irum.productservice.domain.product.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@Getter
@RequiredArgsConstructor
public class ProductDeletedEvent {
    private final UUID productId;
    private final Long deletedBy;
}
