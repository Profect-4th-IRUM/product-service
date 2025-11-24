package com.irum.productservice.domain.review.event;


import com.irum.global.advice.exception.CommonException;
import com.irum.productservice.domain.product.domain.entity.Product;
import com.irum.productservice.domain.product.domain.repository.ProductRepository;
import com.irum.productservice.domain.product.dto.response.ProductResponse;
import com.irum.productservice.domain.product.service.ProductService;
import com.irum.productservice.domain.review.domain.entity.Review;
import com.irum.productservice.domain.review.domain.repository.ReviewRepository;
import com.irum.productservice.global.exception.errorcode.ProductErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReviewEventListener {

    private final ReviewRepository reviewRepository;
    private final ProductService productService;
    private final ProductRepository productRepository;

    @Async
    @EventListener
    public void onReviewCreatedEvent(ReviewCreatedEvent event) {
        UUID productId = event.getProductId();

        Integer count = reviewRepository.findCountByProductId(productId);
        Double avg = reviewRepository.findAverageByProductId(productId);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new CommonException(ProductErrorCode.PRODUCT_NOT_FOUND));
        product.updateRating(avg, count);
        productRepository.save(product);
        log.info("리뷰 통계 업데이트 완료: productId={}, count={}, avg={}",
                productId, count, avg);
    }
    @Async
    @EventListener
    public void onReviewUpdatedEvent(ReviewUpdatedEvent event) {

        UUID productId = event.getProductId();

        Integer count = reviewRepository.findCountByProductId(productId);
        Double avg = reviewRepository.findAverageByProductId(productId);

        log.info("리뷰 수정 이벤트 수신 → 리뷰 통계 업데이트: productId={}, count={}, avg={}",
                productId, count, avg);
    }
    @Async
    @EventListener
    public void onReviewDeletedEvent(ReviewDeletedEvent event) {

        UUID productId = event.getProductId();

        Integer count = reviewRepository.findCountByProductId(productId);
        Double avg   = reviewRepository.findAverageByProductId(productId);

        log.info("[ReviewDeletedEvent] 통계 재계산 완료 → productId={}, count={}, avg={}",
                productId, count, avg);
    }

}
