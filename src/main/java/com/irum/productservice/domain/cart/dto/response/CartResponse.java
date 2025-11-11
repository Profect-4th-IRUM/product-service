// package com.irum.productservice.domain.cart.dto.response;
//
// import com.irum.productservice.domain.cart.domain.entity.Cart;
// import com.irum.productservice.domain.product.domain.entity.ProductImage;
// import java.util.UUID;
// import lombok.Builder;
//
// @Builder
// public record CartResponse(
//        UUID cartId,
//        UUID optionValueId,
//        String productName,
//        String optionValueName,
//        String imageUrl,
//        int quantity,
//        int basePrice, // 상품 기본가
//        int extraPrice, // 옵션 추가금
//        int discountAmount, // 할인 금액
//        int unitPrice, // 단가 (base + extra - discount)
//        int lineTotal // 총액 (unitPrice * quantity)
//        ) {
//    public static CartResponse from(Cart cart) {
//        var optionValue = cart.getOptionValue();
//        var product = optionValue.getOptionGroup().getProduct();
//
//        int base = product.getPrice();
//        int extra = optionValue.getExtraPrice() == null ? 0 : optionValue.getExtraPrice();
//        int discountAmount =
//                (product.getDiscount() != null) ? product.getDiscount().getAmount() : 0;
//
//        int unit = Math.max(base + extra - discountAmount, 0); // 음수 방지
//        int total = unit * cart.getQuantity();
//
//        String imageUrl =
//                product.getProductImages().stream()
//                        .findFirst()
//                        .map(ProductImage::getImageUrl)
//                        .orElse(null);
//
//        return CartResponse.builder()
//                .cartId(cart.getId())
//                .optionValueId(optionValue.getId())
//                .productName(product.getName())
//                .optionValueName(optionValue.getName())
//                .imageUrl(imageUrl)
//                .quantity(cart.getQuantity())
//                .basePrice(base)
//                .extraPrice(extra)
//                .discountAmount(discountAmount)
//                .unitPrice(unit)
//                .lineTotal(total)
//                .build();
//    }
// }
