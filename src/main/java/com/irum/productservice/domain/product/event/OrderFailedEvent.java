package com.irum.productservice.domain.product.event;

import java.util.List;
import java.util.UUID;

public record OrderFailedEvent(List<OptionValueRequest> optionValueList){
    public record OptionValueRequest(UUID optionValueId, int quantity){

    }

    public static OrderFailedEvent from(List<OptionValueRequest> optionValueList) {
        return new OrderFailedEvent(optionValueList);
    }
}
