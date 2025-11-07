package com.irum.productservice.domain.product.event;

import com.irum.productservice.domain.discount.service.DiscountService;

import com.irum.productservice.domain.product.service.ProductImageService;
import com.irum.productservice.domain.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProductEventListener {
    private final ProductService productService;
    private final DiscountService discountService;
//    private final ReviewService reviewService;
    private final ProductImageService productImageService;

    @EventListener
    public void handleProductDeleted(ProductDeletedEvent event) {
        productService.deleteProductOptionGroupByProductId(event.getProductId(),event.getDeletedBy());
        productImageService.deleteProductImagesByProductId(event.getProductId(),event.getDeletedBy());
//        reviewService.deleteReviewByProductId(event.getProductId(), event.getDeletedBy());
        discountService.deleteDiscountByProductId(event.getProductId(),event.getDeletedBy());
    }


}
