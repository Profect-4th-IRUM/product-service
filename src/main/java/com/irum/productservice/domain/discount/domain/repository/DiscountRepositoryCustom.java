package com.irum.productservice.domain.discount.domain.repository;

import com.irum.productservice.domain.discount.dto.response.DiscountInfoResponse;
import java.util.List;
import java.util.UUID;

public interface DiscountRepositoryCustom {
    List<DiscountInfoResponse> findDiscountListByCursor(UUID storeId, UUID cursor, int pageSize);
}
