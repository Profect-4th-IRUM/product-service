package com.irum.productservice.domain.product.mapper;

import com.irum.openfeign.product.dto.response.ProductDto;
import com.irum.productservice.domain.discount.domain.entity.Discount;
import com.irum.productservice.domain.product.domain.entity.Product;
import com.irum.productservice.domain.product.domain.entity.ProductOptionValue;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {

    public ProductDto toDto(
            Product product, List<ProductOptionValue> optionValues, Discount discount) {
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
                toOptionDtoList(optionValues),
                toDiscountDto(discount));
    }

    private List<ProductDto.ProductOptionDto> toOptionDtoList(
            List<ProductOptionValue> optionValues) {
        return optionValues.stream().map(this::toOptionDto).toList();
    }

    private ProductDto.ProductOptionDto toOptionDto(ProductOptionValue option) {
        return new ProductDto.ProductOptionDto(
                option.getId(),
                option.getName(),
                option.getExtraPrice(),
                option.getStockQuantity());
    }

    private ProductDto.DiscountDto toDiscountDto(Discount discount) {
        if (discount == null) {
            return null;
        }

        return new ProductDto.DiscountDto(
                discount.getId(), discount.getName(), discount.getAmount());
    }
}
