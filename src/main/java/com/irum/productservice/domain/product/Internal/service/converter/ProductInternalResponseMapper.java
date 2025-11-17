package com.irum.productservice.domain.product.Internal.service.converter;

import com.irum.openfeign.product.dto.response.ProductInternalResponse;
import com.irum.productservice.domain.product.domain.entity.ProductOptionValue;
import com.irum.productservice.domain.store.domain.entity.Store;
import lombok.Builder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class ProductInternalResponseMapper {


    public static ProductInternalResponse.ProductResponse toProductResponse(ProductOptionValue pov, Map<UUID, Integer> discountMap) {
                return new ProductInternalResponse.ProductResponse(
                        pov.getOptionGroup().getProduct().getId(),
                        pov.getId(),
                        pov.getOptionGroup().getProduct().getPrice(),
                        pov.getExtraPrice(),
                        discountMap.getOrDefault(
                                pov.getOptionGroup().getProduct().getId(), 0),
                        pov.getName(),
                        pov.getOptionGroup().getProduct().getName()
                );
            }


        public static ProductInternalResponse toProductInternalResponse(
                Store store,
                List<ProductOptionValue> productOptionValueList,
                Map<UUID, Integer> discountMap) {
            List<ProductInternalResponse.ProductResponse> productDtoList =
                    productOptionValueList.stream()
                            .map(pov -> toProductResponse(pov, discountMap))
                            .toList();

            return new ProductInternalResponse(store.getDeliveryPolicy().getDefaultDeliveryFee(),
                    store.getDeliveryPolicy().getMinAmount(),
                    store.getDeliveryPolicy().getMinQuantity(),
                    store.getId(),
                    productDtoList);
        }

}
