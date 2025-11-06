package com.irum.openfeign.dto.response;

import com.irum.productservice.domain.store.domain.entity.Store;
import com.irum.productservice.domain.deliverypolicy.domain.entity.DeliveryPolicy;
import java.util.UUID;

public record StoreDto(
        UUID id,
        String name,
        String contact,
        String address,
        String businessRegistrationNumber,
        String telemarketingRegistrationNumber,
        int defaultDeliveryFee,
        int minAmount,
        int minQuantity
) {
    public static StoreDto from(Store store) {
        DeliveryPolicy policy = store.getDeliveryPolicy();
        return new StoreDto(
                store.getId(),
                store.getName(),
                store.getContact(),
                store.getAddress(),
                store.getBusinessRegistrationNumber(),
                store.getTelemarketingRegistrationNumber(),
                policy.getDefaultDeliveryFee(),
                policy.getMinAmount(),
                policy.getMinQuantity()
        );
    }
}
