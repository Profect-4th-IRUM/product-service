package com.irum.productservice.domain.product.event;

import com.irum.productservice.domain.discount.service.DiscountService;
import com.irum.productservice.domain.product.service.ProductImageService;
import com.irum.productservice.domain.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductEventListener {
    private final ProductService productService;
    private final DiscountService discountService;
    //    private final ReviewService reviewService;
    private final ProductImageService productImageService;
    private final RedisTemplate<Object, Object> redisTemplate;

    @EventListener
    public void handleProductDeleted(ProductDeletedEvent event) {
        productService.deleteProductOptionGroupByProductId(
                event.getProductId(), event.getDeletedBy());
        productImageService.deleteProductImagesByProductId(
                event.getProductId(), event.getDeletedBy());
        //        reviewService.deleteReviewByProductId(event.getProductId(), event.getDeletedBy());
        discountService.deleteDiscountByProductId(event.getProductId(), event.getDeletedBy());
    }

    @EventListener
    public void handleOptionGroupDeleted(OptionGroupDeletedEvent event) {
        productService.deleteOptionValueByOptionGroupId(
                event.getOptionGroupId(), event.getDeletedBy());
    }

    @EventListener
    public void handleVisibilityChanged(ProductVisibilityChangedEvent event) {
        String key = "product : " + event.getProductId();
        redisTemplate.opsForHash().put(key, "isPublic", event.isPublic());
        log.info(
                "Redis 상품 공개상태 업데이트 완료: productId={}, isPublic={}",
                event.getProductId(),
                event.isPublic());
    }
}
