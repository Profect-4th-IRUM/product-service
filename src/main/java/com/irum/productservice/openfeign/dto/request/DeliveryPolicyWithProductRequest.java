package com.irum.productservice.openfeign.dto.request;

import java.util.List;
import java.util.UUID;

public record DeliveryPolicyWithProductRequest (
        List<UUID>optionValueIdList, UUID storeId
) {
}
