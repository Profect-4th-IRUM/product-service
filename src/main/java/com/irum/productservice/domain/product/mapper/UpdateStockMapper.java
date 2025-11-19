package com.irum.productservice.domain.product.mapper;

import com.irum.openfeign.product.dto.response.UpdateStockDto;
import com.irum.productservice.domain.product.domain.entity.Product;
import com.irum.productservice.domain.product.domain.entity.ProductOptionValue;
import com.irum.productservice.domain.store.domain.entity.Store;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class UpdateStockMapper {

    public UpdateStockDto toDto(
            Store store,
            List<ProductOptionValue> optionValues,
            Map<UUID, Integer> discountMap // ← 할인 맵으로 변경
            ) {
        return UpdateStockDto.builder()
                .defaultDeliveryFee(store.getDeliveryPolicy().getDefaultDeliveryFee())
                .minAmount(store.getDeliveryPolicy().getMinAmount())
                .minQuantity(store.getDeliveryPolicy().getMinQuantity())
                .storeId(store.getId())
                .productList(toProductDtoList(optionValues, discountMap))
                .build();
    }

    // 옵션 목록 → ProductDto 목록
    private List<UpdateStockDto.ProductDto> toProductDtoList(
            List<ProductOptionValue> optionValues, Map<UUID, Integer> discountMap) {
        return optionValues.stream().map(option -> toProductDto(option, discountMap)).toList();
    }

    // 단일 옵션 → ProductDto 변환
    private UpdateStockDto.ProductDto toProductDto(
            ProductOptionValue option, Map<UUID, Integer> discountMap) {
        Product product = option.getOptionGroup().getProduct();
        int discountAmount = discountMap.getOrDefault(product.getId(), 0); // ← Map 사용!

        return UpdateStockDto.ProductDto.builder()
                .productId(product.getId())
                .optionValueId(option.getId())
                .price(product.getPrice())
                .extraPrice(option.getExtraPrice())
                .productDiscount(discountAmount)
                .optionName(option.getName())
                .productName(product.getName())
                .build();
    }
}
