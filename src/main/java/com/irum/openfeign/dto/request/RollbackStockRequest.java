package com.irum.openfeign.dto.request;

import java.util.List;
import java.util.UUID;

public record RollbackStockRequest (
        List<OptionValueRequest> optionValueList
) {
    public record OptionValueRequest(
            UUID optionValueId,
            int quantity
    ) {
    }
}