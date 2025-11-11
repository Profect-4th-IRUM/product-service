package com.irum.productservice.domain.product.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@Getter
@RequiredArgsConstructor
public class OptionGroupDeletedEvent {
    private final UUID optionGroupId;
    private final Long deletedBy;
}
