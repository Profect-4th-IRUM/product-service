package com.irum.productservice.domain.cart.dto.response;

import com.irum.productservice.domain.cart.domain.entity.CartRedis;
import com.irum.productservice.domain.product.domain.entity.ProductImage;
import com.irum.productservice.domain.product.domain.entity.ProductOptionValue;
import java.util.UUID;
import lombok.Builder;

@Builder
public record CartResponse(
        UUID cartId,
        UUID optionValueId,
        String productName,
        String optionValueName,
        String imageUrl,
        int quantity,
        int basePrice, // 상품 기본가
        int extraPrice, // 옵션 추가금
        int discountAmount, // 할인 금액
        int unitPrice, // 단가 (base + extra - discount)
        int lineTotal // 총액 (unitPrice * quantity)
        ) {

    /** Redis 장바구니 데이터 + 상품 옵션 + 할인 금액을 기반으로 CartResponse 생성 */
    public static CartResponse from(
            CartRedis cart, ProductOptionValue optionValue, int discountAmount) {
        var product = optionValue.getOptionGroup().getProduct();

        int base = product.getPrice();
        int extra = optionValue.getExtraPrice() == null ? 0 : optionValue.getExtraPrice();
        int discount = Math.max(discountAmount, 0);

        int unit = Math.max(base + extra - discount, 0);
        int total = unit * cart.getQuantity();

        // 대표 이미지 (기본이미지 우선)
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
                .cartId(cart.getCartId())
                .optionValueId(optionValue.getId())
                .productName(product.getName())
                .optionValueName(optionValue.getName())
                .imageUrl(imageUrl)
                .quantity(cart.getQuantity())
                .basePrice(base)
                .extraPrice(extra)
                .discountAmount(discount)
                .unitPrice(unit)
                .lineTotal(total)
                .build();
    }
}
