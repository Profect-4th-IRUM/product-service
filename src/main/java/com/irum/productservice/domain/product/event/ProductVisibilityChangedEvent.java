package com.irum.productservice.domain.product.event;

import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ProductVisibilityChangedEvent {
    private final UUID ProductId;
    private final boolean isPublic;
}
