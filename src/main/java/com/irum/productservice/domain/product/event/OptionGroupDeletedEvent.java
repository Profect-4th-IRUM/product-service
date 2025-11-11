package com.irum.productservice.domain.product.event;

import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class OptionGroupDeletedEvent {
    private final UUID optionGroupId;
    private final Long deletedBy;
}
