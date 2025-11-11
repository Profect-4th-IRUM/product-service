package com.irum.productservice.domain.store.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@Getter
@RequiredArgsConstructor
public class StoreDeletedEvent {
    private final UUID storeId;
    private final Long deletedBy;
}
