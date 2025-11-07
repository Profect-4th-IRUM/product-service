package com.irum.openfeign.dto.request;

import java.util.List;
import java.util.UUID;

public record UpdateStockRequest(
        List<OptionValueRequest>optionValueList, UUID storeId
) {
    public record OptionValueRequest(
            UUID optionValueId,
            int quantity
    ) {
    }
}
