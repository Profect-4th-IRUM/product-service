package com.irum.productservice.domain.review.service;

import com.irum.global.advice.exception.CommonException;
import com.irum.global.context.MemberAuthContext;
import com.irum.openfeign.client.OrderClient;
import com.irum.openfeign.dto.response.OrderDetailDto;
import com.irum.productservice.domain.product.domain.entity.Product;
import com.irum.productservice.domain.product.domain.repository.ProductRepository;
import com.irum.productservice.domain.review.domain.entity.Review;
import com.irum.productservice.domain.review.domain.entity.ReviewImage;
import com.irum.productservice.domain.review.domain.repository.ReviewImageRepository;
import com.irum.productservice.domain.review.domain.repository.ReviewRepository;
import com.irum.productservice.domain.review.dto.request.ReviewCreateRequest;
import com.irum.productservice.domain.review.dto.request.ReviewUpdateRequest;
import com.irum.productservice.domain.review.dto.response.ReviewResponse;
import com.irum.productservice.global.exception.errorcode.ProductErrorCode;
import com.irum.productservice.global.exception.errorcode.ReviewErrorCode;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ReviewImageRepository reviewImageRepository;
    private final ProductRepository productRepository;
    private final OrderClient orderClient;

    /** 리뷰 생성 */
    public ReviewResponse createReview(ReviewCreateRequest request) {
        Long currentMemberId = MemberAuthContext.getMemberId();

        OrderDetailDto orderDetail = orderClient.getOrderDetail(request.orderDetailId());

        if (!orderDetail.memberId().equals(currentMemberId)) {
            throw new CommonException(ReviewErrorCode.REVIEW_UNAUTHORIZED);
        }

        if (!"DELIVERED".equals(orderDetail.orderStatus())) {
            throw new CommonException(ReviewErrorCode.NOT_REVIEWABLE_ORDER);
        }

        if (reviewRepository.existsByOrderDetailId(orderDetail.orderDetailId())) {
            throw new CommonException(ReviewErrorCode.REVIEW_ALREADY_EXISTS);
        }

        Product product =
                productRepository
                        .findById(orderDetail.productId())
                        .orElseThrow(() -> new CommonException(ProductErrorCode.PRODUCT_NOT_FOUND));

        Review review =
                Review.createReview(
                        request.content(),
                        request.rate(),
                        currentMemberId,
                        product,
                        orderDetail.orderDetailId());

        Review saved = reviewRepository.save(review);

        List<ReviewImage> images =
                (request.imageUrls() == null)
                        ? List.of()
                        : request.imageUrls().stream()
                                .map(url -> ReviewImage.create(url, saved))
                                .toList();

        reviewImageRepository.saveAll(images);

        updateProductRating(product);

        log.info(
                "리뷰 생성 완료: memberId={}, orderDetailId={}, productId={}",
                currentMemberId,
                orderDetail.orderDetailId(),
                product.getId());

        return ReviewResponse.from(saved, images.stream().map(ReviewImage::getImageUrl).toList());
    }

    /** 리뷰 수정 */
    public ReviewResponse updateReview(UUID reviewId, ReviewUpdateRequest request) {
        Review review =
                reviewRepository
                        .findById(reviewId)
                        .orElseThrow(() -> new CommonException(ReviewErrorCode.REVIEW_NOT_FOUND));

        if (!review.getMemberId().equals(MemberAuthContext.getMemberId())) {
            throw new CommonException(ReviewErrorCode.REVIEW_UNAUTHORIZED);
        }

        boolean noContentChange = request.content() == null;
        boolean noRateChange = request.rate() == null;
        boolean noImageChange = request.imageUrls() == null;

        if (noContentChange && noRateChange && noImageChange) {
            throw new CommonException(ReviewErrorCode.REVIEW_NOT_MODIFIED);
        }

        review.updateReview(request.content(), request.rate());

        if (request.imageUrls() != null) {
            reviewImageRepository
                    .findAllByReview(review)
                    .forEach(img -> img.softDelete(MemberAuthContext.getMemberId()));

            List<ReviewImage> newImages =
                    request.imageUrls().stream()
                            .map(url -> ReviewImage.create(url, review))
                            .toList();

            reviewImageRepository.saveAll(newImages);
        }

        List<String> imageUrls =
                reviewImageRepository.findAllByReview(review).stream()
                        .map(ReviewImage::getImageUrl)
                        .toList();

        if (request.rate() != null) {
            updateProductRating(review.getProduct());
        }

        return ReviewResponse.from(review, imageUrls);
    }

    /** 내 리뷰 목록 조회 */
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getMyReviews(Pageable pageable) {
        Long currentMemberId = MemberAuthContext.getMemberId();

        return reviewRepository
                .findAllByMemberId(currentMemberId, pageable)
                .map(
                        review -> {
                            List<String> urls =
                                    reviewImageRepository.findAllByReview(review).stream()
                                            .map(ReviewImage::getImageUrl)
                                            .toList();
                            return ReviewResponse.from(review, urls);
                        });
    }

    /** 상품 리뷰 목록 조회 */
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getProductReviews(UUID productId, Pageable pageable) {
        return reviewRepository
                .findAllByProduct_Id(productId, pageable)
                .map(
                        review -> {
                            List<String> urls =
                                    reviewImageRepository.findAllByReview(review).stream()
                                            .map(ReviewImage::getImageUrl)
                                            .toList();
                            return ReviewResponse.from(review, urls);
                        });
    }

    /** 리뷰 삭제 */
    public void deleteReview(UUID reviewId) {
        Review review =
                reviewRepository
                        .findById(reviewId)
                        .orElseThrow(() -> new CommonException(ReviewErrorCode.REVIEW_NOT_FOUND));

        if (!review.getMemberId().equals(MemberAuthContext.getMemberId())) {
            throw new CommonException(ReviewErrorCode.REVIEW_UNAUTHORIZED);
        }

        reviewImageRepository
                .findAllByReview(review)
                .forEach(img -> img.softDelete(MemberAuthContext.getMemberId()));

        review.softDelete(MemberAuthContext.getMemberId());

        updateProductRating(review.getProduct());

        log.info("리뷰 삭제 완료: reviewId={}", reviewId);
    }

    /** 상품 평점 갱신 */
    private void updateProductRating(Product product) {
        Double avg = reviewRepository.findAverageByProductId(product.getId());
        Integer count = reviewRepository.findCountByProductId(product.getId());

        product.updateRating(avg, count);
        productRepository.save(product);

        log.info(
                "상품 평점 갱신 완료: productId={}, avgRate={}, reviewCount={}",
                product.getId(),
                avg,
                count);
    }
}
