package com.irum.productservice.domain.cart.mapper;

import com.irum.productservice.domain.cart.domain.entity.CartItem;
import com.irum.productservice.domain.cart.dto.response.CartResponse;
import com.irum.productservice.domain.product.domain.entity.ProductImage;
import com.irum.productservice.domain.product.domain.entity.ProductOptionValue;
import org.springframework.stereotype.Component;

@Component
public class CartMapper {

    /** Redis 장바구니 데이터 + 상품 옵션 + 할인 금액 결합 */
    public CartResponse toResponse(
            CartItem cartItem, ProductOptionValue optionValue, int discountAmount) {

        var product = optionValue.getOptionGroup().getProduct();

        int base = product.getPrice();
        int extra = optionValue.getExtraPrice() == null ? 0 : optionValue.getExtraPrice();
        int discount = Math.max(discountAmount, 0);

        int unit = Math.max(base + extra - discount, 0);
        int total = unit * cartItem.getQuantity();

        int stockQuantity = optionValue.getStockQuantity();

        String imageUrl =
                product.getProductImages().stream()
                        .filter(ProductImage::isDefault)
                        .findFirst()
                        .map(ProductImage::getImageUrl)
                        .orElseGet(
                                () ->
                                        product.getProductImages().stream()
                                                .findFirst()
                                                .map(ProductImage::getImageUrl)
                                                .orElse(null));

        return CartResponse.builder()
                .cartItemId(cartItem.getCartItemId())
                .optionValueId(optionValue.getId())
                .productName(product.getName())
                .optionValueName(optionValue.getName())
                .imageUrl(imageUrl)
                .quantity(cartItem.getQuantity())
                .basePrice(base)
                .extraPrice(extra)
                .discountAmount(discount)
                .unitPrice(unit)
                .lineTotal(total)
                .stockQuantity(stockQuantity)
                .build();
    }
}
