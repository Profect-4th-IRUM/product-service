package com.irum.productservice.domain.store.event;

import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class StoreDeletedEvent {
    private final UUID storeId;
    private final Long deletedBy;
}
