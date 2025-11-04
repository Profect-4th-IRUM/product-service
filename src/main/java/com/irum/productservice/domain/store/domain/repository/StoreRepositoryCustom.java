package com.irum.productservice.domain.store.domain.repository;

import com.irum.productservice.domain.store.dto.response.StoreListResponse;
import java.util.List;
import java.util.UUID;

public interface StoreRepositoryCustom {
    List<StoreListResponse> findStoresByCursor(UUID cursor, int size);
}
