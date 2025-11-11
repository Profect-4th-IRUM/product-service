package com.irum.productservice.domain.product.event;

import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ProductDeletedEvent {
    private final UUID productId;
    private final Long deletedBy;
}
