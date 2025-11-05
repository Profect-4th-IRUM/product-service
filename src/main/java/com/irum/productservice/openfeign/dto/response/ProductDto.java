package com.irum.productservice.openfeign.dto.response;

import com.irum.productservice.domain.discount.domain.entity.Discount;
import com.irum.productservice.domain.product.domain.entity.Product;
import com.irum.productservice.domain.product.domain.entity.ProductOptionValue;
import java.util.List;
import java.util.UUID;

public record ProductDto(
        UUID productId,
        String productName,
        String description,
        String detailDescription,
        int price,
        boolean isPublic,
        Double avgRating,
        Integer reviewCount,
        UUID categoryId,
        String categoryName,
        UUID storeId,
        List<ProductOptionDto> options,
        DiscountDto discount
) {
    public static ProductDto from(Product product, List<ProductOptionValue> optionValues,Discount discount) {
        return new ProductDto(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getDetailDescription(),
                product.getPrice(),
                product.isPublic(),
                product.getAvgRating(),
                product.getReviewCount(),
                product.getCategory().getCategoryId(),
                product.getCategory().getName(),
                product.getStore().getId(),
                optionValues.stream()
                        .map(ProductOptionDto::from)
                        .toList(),
                DiscountDto.from(discount)
        );
    }

    public record ProductOptionDto(
            UUID optionValueId,
            String optionName,
            int extraPrice,
            int stockQuantity
    ) {
        public static ProductOptionDto from(ProductOptionValue optionValue) {
            return new ProductOptionDto(
                    optionValue.getId(),
                    optionValue.getName(),
                    optionValue.getExtraPrice(),
                    optionValue.getStockQuantity()
            );
        }
    }
    public record DiscountDto (
            UUID discountId,
            String name,
            int amount
    ){
        public static DiscountDto from(Discount discount){
            if (discount == null) {
                return null;
            }
            return new DiscountDto(
                    discount.getId(),
                    discount.getName(),
                    discount.getAmount()
            );
        }

    }

}
