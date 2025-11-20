package com.irum.productservice.domain.store.mapper;

import com.irum.openfeign.product.dto.response.StoreDto;
import com.irum.productservice.domain.store.domain.entity.Store;
import org.springframework.stereotype.Component;

@Component
public class StoreMapper {

    public StoreDto toDto(Store store) {
        return new StoreDto(
                store.getId(),
                store.getName(),
                store.getContact(),
                store.getAddress(),
                store.getBusinessRegistrationNumber(),
                store.getTelemarketingRegistrationNumber(),
                store.getDeliveryPolicy().getDefaultDeliveryFee(),
                store.getDeliveryPolicy().getMinAmount(),
                store.getDeliveryPolicy().getMinQuantity());
    }
}
