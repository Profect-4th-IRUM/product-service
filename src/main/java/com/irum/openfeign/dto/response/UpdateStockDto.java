package com.irum.productservice.domain.openfeign.dto.response;

import com.irum.productservice.domain.product.domain.entity.ProductOptionValue;
import com.irum.productservice.domain.store.domain.entity.Store;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Builder;

@Builder
public record UpdateStockDto(
        int defaultDeliveryFee,
        int minAmount,
        int minQuantity,
        UUID storeId,
        List<ProductDto> productList) {
    @Builder
    public record ProductDto(
            UUID productId,
            UUID optionValueId,
            int price,
            int extraPrice,
            int productDiscount,
            String optionName,
            String productName) {

        public static ProductDto from(ProductOptionValue pov, Map<UUID, Integer> discountMap) {
            return ProductDto.builder()
                    .productId(pov.getOptionGroup().getProduct().getId())
                    .optionValueId(pov.getId())
                    .price(pov.getOptionGroup().getProduct().getPrice())
                    .extraPrice(pov.getExtraPrice())
                    .productDiscount(
                            discountMap.getOrDefault(
                                    pov.getOptionGroup().getProduct().getId(), 0)) // 없으면 0
                    .optionName(pov.getName())
                    .productName(pov.getOptionGroup().getProduct().getName())
                    .build();
        }
    }

    public static UpdateStockDto from(
            Store store,
            List<ProductOptionValue> productOptionValueList,
            Map<UUID, Integer> discountMap) {
        List<ProductDto> productDtoList =
                productOptionValueList.stream()
                        .map(pov -> ProductDto.from(pov, discountMap))
                        .toList();

        return UpdateStockDto.builder()
                .defaultDeliveryFee(store.getDeliveryPolicy().getDefaultDeliveryFee())
                .minAmount(store.getDeliveryPolicy().getMinAmount())
                .minQuantity(store.getDeliveryPolicy().getMinQuantity())
                .storeId(store.getId())
                .productList(productDtoList)
                .build();
    }
}
