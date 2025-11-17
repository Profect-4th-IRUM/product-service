package com.irum.productservice.domain.review.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

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
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    private ReviewRepository reviewRepository;
    private ReviewImageRepository reviewImageRepository;
    private ProductRepository productRepository;
    private OrderClient orderClient;

    private ReviewService reviewService;

    private UUID productId;
    private UUID orderDetailId;
    private Long memberId;

    @BeforeEach
    void setUp() {
        reviewRepository = Mockito.mock(ReviewRepository.class);
        reviewImageRepository = Mockito.mock(ReviewImageRepository.class);
        productRepository = Mockito.mock(ProductRepository.class);
        orderClient = Mockito.mock(OrderClient.class);

        reviewService =
                new ReviewService(
                        reviewRepository, reviewImageRepository, productRepository, orderClient);

        productId = UUID.randomUUID();
        orderDetailId = UUID.randomUUID();
        memberId = 1L;
    }

    /* ==============================
     *  리뷰 생성(createReview)
     * ============================== */

    @Test
    @DisplayName("리뷰 생성 성공 - 본인 주문 & 배송완료 & 리뷰 중복 없음")
    void createReview_success() {
        // given
        ReviewCreateRequest request =
                new ReviewCreateRequest(
                        orderDetailId,
                        productId,
                        "맛있어요",
                        (short) 5,
                        List.of("https://example.com/review1.jpg"));

        OrderDetailDto orderDetailDto =
                new OrderDetailDto(orderDetailId, productId, memberId, "DELIVERED");

        Product product = Mockito.mock(Product.class);
        // product.getId() 호출에 대한 스텁
        when(product.getId()).thenReturn(productId);

        Review savedReview =
                Review.createReview(
                        request.content(), request.rate(), memberId, product, orderDetailId);

        try (MockedStatic<MemberAuthContext> mockedStatic =
                Mockito.mockStatic(MemberAuthContext.class)) {

            mockedStatic.when(MemberAuthContext::getMemberId).thenReturn(memberId);

            when(orderClient.getOrderDetail(orderDetailId)).thenReturn(orderDetailDto);
            when(reviewRepository.existsByOrderDetailId(orderDetailId)).thenReturn(false);
            when(productRepository.findById(productId)).thenReturn(Optional.of(product));
            when(reviewRepository.save(any(Review.class))).thenReturn(savedReview);
            when(reviewRepository.findAverageByProductId(any())).thenReturn(5.0);
            when(reviewRepository.findCountByProductId(any())).thenReturn(1);

            // when
            ReviewResponse response = reviewService.createReview(request);

            // then
            assertThat(response).isNotNull();
            // reviewId는 DB에서 채워지는 값이라 단위테스트에선 null이어도 상관 없음
            assertThat(response.productId()).isEqualTo(productId);
            assertThat(response.memberId()).isEqualTo(memberId);
            assertThat(response.rate()).isEqualTo((short) 5);

            verify(reviewRepository).save(any(Review.class));
            verify(reviewImageRepository).saveAll(anyList());
            verify(productRepository).save(any(Product.class));
        }
    }

    @Test
    @DisplayName("리뷰 생성 실패 - 다른 사람의 주문")
    void createReview_fail_unauthorized() {
        // given
        ReviewCreateRequest request =
                new ReviewCreateRequest(
                        orderDetailId,
                        productId,
                        "맛있어요",
                        (short) 5,
                        List.of("https://example.com/review1.jpg"));

        OrderDetailDto orderDetailDto =
                new OrderDetailDto(orderDetailId, productId, 999L, "DELIVERED");

        try (MockedStatic<MemberAuthContext> mockedStatic =
                Mockito.mockStatic(MemberAuthContext.class)) {

            mockedStatic.when(MemberAuthContext::getMemberId).thenReturn(memberId);

            when(orderClient.getOrderDetail(orderDetailId)).thenReturn(orderDetailDto);

            // when & then
            CommonException ex =
                    assertThrows(CommonException.class, () -> reviewService.createReview(request));

            assertThat(ex.getErrorCode()).isEqualTo(ReviewErrorCode.REVIEW_UNAUTHORIZED);
        }
    }

    @Test
    @DisplayName("리뷰 생성 실패 - 배송완료 상태 아님")
    void createReview_fail_notDelivered() {
        // given
        ReviewCreateRequest request =
                new ReviewCreateRequest(
                        orderDetailId,
                        productId,
                        "맛있어요",
                        (short) 5,
                        List.of("https://example.com/review1.jpg"));

        OrderDetailDto orderDetailDto =
                new OrderDetailDto(orderDetailId, productId, memberId, "SHIPPED");

        try (MockedStatic<MemberAuthContext> mockedStatic =
                Mockito.mockStatic(MemberAuthContext.class)) {

            mockedStatic.when(MemberAuthContext::getMemberId).thenReturn(memberId);

            when(orderClient.getOrderDetail(orderDetailId)).thenReturn(orderDetailDto);

            // when & then
            CommonException ex =
                    assertThrows(CommonException.class, () -> reviewService.createReview(request));

            assertThat(ex.getErrorCode()).isEqualTo(ReviewErrorCode.NOT_REVIEWABLE_ORDER);
        }
    }

    @Test
    @DisplayName("리뷰 생성 실패 - 이미 리뷰 존재")
    void createReview_fail_alreadyExists() {
        // given
        ReviewCreateRequest request =
                new ReviewCreateRequest(
                        orderDetailId,
                        productId,
                        "맛있어요",
                        (short) 5,
                        List.of("https://example.com/review1.jpg"));

        OrderDetailDto orderDetailDto =
                new OrderDetailDto(orderDetailId, productId, memberId, "DELIVERED");

        try (MockedStatic<MemberAuthContext> mockedStatic =
                Mockito.mockStatic(MemberAuthContext.class)) {

            mockedStatic.when(MemberAuthContext::getMemberId).thenReturn(memberId);

            when(orderClient.getOrderDetail(orderDetailId)).thenReturn(orderDetailDto);
            when(reviewRepository.existsByOrderDetailId(orderDetailId)).thenReturn(true);

            // when & then
            CommonException ex =
                    assertThrows(CommonException.class, () -> reviewService.createReview(request));

            assertThat(ex.getErrorCode()).isEqualTo(ReviewErrorCode.REVIEW_ALREADY_EXISTS);
        }
    }

    @Test
    @DisplayName("리뷰 생성 실패 - 상품 없음")
    void createReview_fail_productNotFound() {
        // given
        ReviewCreateRequest request =
                new ReviewCreateRequest(
                        orderDetailId,
                        productId,
                        "맛있어요",
                        (short) 5,
                        List.of("https://example.com/review1.jpg"));

        OrderDetailDto orderDetailDto =
                new OrderDetailDto(orderDetailId, productId, memberId, "DELIVERED");

        try (MockedStatic<MemberAuthContext> mockedStatic =
                Mockito.mockStatic(MemberAuthContext.class)) {

            mockedStatic.when(MemberAuthContext::getMemberId).thenReturn(memberId);

            when(orderClient.getOrderDetail(orderDetailId)).thenReturn(orderDetailDto);
            when(reviewRepository.existsByOrderDetailId(orderDetailId)).thenReturn(false);
            when(productRepository.findById(productId)).thenReturn(Optional.empty());

            // when & then
            CommonException ex =
                    assertThrows(CommonException.class, () -> reviewService.createReview(request));

            assertThat(ex.getErrorCode()).isEqualTo(ProductErrorCode.PRODUCT_NOT_FOUND);
        }
    }

    /* ==============================
     *  리뷰 수정(updateReview)
     * ============================== */

    @Test
    @DisplayName("리뷰 수정 성공 - 내용/평점/이미지 변경")
    void updateReview_success() {
        // given
        UUID reviewId = UUID.randomUUID();
        Product product = Mockito.mock(Product.class);
        when(product.getId()).thenReturn(productId);

        Review review =
                Review.createReview("old content", (short) 3, memberId, product, orderDetailId);

        ReviewUpdateRequest request =
                new ReviewUpdateRequest(
                        "new content",
                        (short) 5,
                        List.of("https://example.com/new1.jpg", "https://example.com/new2.jpg"));

        try (MockedStatic<MemberAuthContext> mockedStatic =
                Mockito.mockStatic(MemberAuthContext.class)) {

            mockedStatic.when(MemberAuthContext::getMemberId).thenReturn(memberId);

            when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));

            // 첫 호출은 soft delete용, 두 번째 호출은 응답용
            ReviewImage oldImage = ReviewImage.create("old-url", review);
            List<ReviewImage> newImages =
                    List.of(
                            ReviewImage.create("https://example.com/new1.jpg", review),
                            ReviewImage.create("https://example.com/new2.jpg", review));

            when(reviewImageRepository.findAllByReview(review))
                    .thenReturn(List.of(oldImage))
                    .thenReturn(newImages);

            when(reviewRepository.findAverageByProductId(any())).thenReturn(5.0);
            when(reviewRepository.findCountByProductId(any())).thenReturn(1);

            // when
            ReviewResponse response = reviewService.updateReview(reviewId, request);

            // then
            assertThat(response.content()).isEqualTo("new content");
            assertThat(response.rate()).isEqualTo((short) 5);
            assertThat(response.imageUrls()).hasSize(2);

            verify(reviewImageRepository).saveAll(anyList());
            verify(productRepository).save(any(Product.class));
        }
    }

    @Test
    @DisplayName("리뷰 수정 실패 - 본인 리뷰 아님")
    void updateReview_fail_unauthorized() {
        // given
        UUID reviewId = UUID.randomUUID();
        Product product = Mockito.mock(Product.class);
        Review review = Review.createReview("old content", (short) 3, 999L, product, orderDetailId);

        ReviewUpdateRequest request = new ReviewUpdateRequest("new", (short) 4, List.of("url"));

        try (MockedStatic<MemberAuthContext> mockedStatic =
                Mockito.mockStatic(MemberAuthContext.class)) {

            mockedStatic.when(MemberAuthContext::getMemberId).thenReturn(memberId);

            when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));

            // when & then
            CommonException ex =
                    assertThrows(
                            CommonException.class,
                            () -> reviewService.updateReview(reviewId, request));

            assertThat(ex.getErrorCode()).isEqualTo(ReviewErrorCode.REVIEW_UNAUTHORIZED);
        }
    }

    @Test
    @DisplayName("리뷰 수정 실패 - 변경 사항 없음")
    void updateReview_fail_notModified() {
        // given
        UUID reviewId = UUID.randomUUID();
        Product product = Mockito.mock(Product.class);
        Review review =
                Review.createReview("old content", (short) 3, memberId, product, orderDetailId);

        ReviewUpdateRequest request = new ReviewUpdateRequest(null, null, null);

        try (MockedStatic<MemberAuthContext> mockedStatic =
                Mockito.mockStatic(MemberAuthContext.class)) {

            mockedStatic.when(MemberAuthContext::getMemberId).thenReturn(memberId);

            when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));

            // when & then
            CommonException ex =
                    assertThrows(
                            CommonException.class,
                            () -> reviewService.updateReview(reviewId, request));

            assertThat(ex.getErrorCode()).isEqualTo(ReviewErrorCode.REVIEW_NOT_MODIFIED);
        }
    }

    /* ==============================
     *  내 리뷰 목록(getMyReviews)
     * ============================== */

    @Test
    @DisplayName("내 리뷰 목록 조회 성공")
    void getMyReviews_success() {
        // given
        Product product = Mockito.mock(Product.class);
        when(product.getId()).thenReturn(productId);

        Review review = Review.createReview("content", (short) 5, memberId, product, orderDetailId);

        PageRequest pageable = PageRequest.of(0, 10);
        Page<Review> reviewPage = new PageImpl<>(List.of(review), pageable, 1);

        ReviewImage reviewImage = ReviewImage.create("https://example.com/review1.jpg", review);

        try (MockedStatic<MemberAuthContext> mockedStatic =
                Mockito.mockStatic(MemberAuthContext.class)) {

            mockedStatic.when(MemberAuthContext::getMemberId).thenReturn(memberId);

            when(reviewRepository.findAllByMemberId(eq(memberId), any())).thenReturn(reviewPage);
            when(reviewImageRepository.findAllByReview(review)).thenReturn(List.of(reviewImage));

            // when
            Page<ReviewResponse> result = reviewService.getMyReviews(pageable);

            // then
            assertThat(result.getTotalElements()).isEqualTo(1);
            ReviewResponse resp = result.getContent().get(0);
            assertThat(resp.memberId()).isEqualTo(memberId);
            assertThat(resp.imageUrls()).hasSize(1);
        }
    }

    /* ==============================
     *  상품 리뷰 목록(getProductReviews)
     * ============================== */

    @Test
    @DisplayName("상품별 리뷰 목록 조회 성공")
    void getProductReviews_success() {
        // given
        Product product = Mockito.mock(Product.class);
        when(product.getId()).thenReturn(productId);

        Review review = Review.createReview("content", (short) 4, memberId, product, orderDetailId);

        PageRequest pageable = PageRequest.of(0, 10);
        Page<Review> reviewPage = new PageImpl<>(List.of(review), pageable, 1);

        ReviewImage reviewImage = ReviewImage.create("https://example.com/review1.jpg", review);

        when(reviewRepository.findAllByProduct_Id(eq(productId), any())).thenReturn(reviewPage);
        when(reviewImageRepository.findAllByReview(review)).thenReturn(List.of(reviewImage));

        // when
        Page<ReviewResponse> result = reviewService.getProductReviews(productId, pageable);

        // then
        assertThat(result.getTotalElements()).isEqualTo(1);
        ReviewResponse resp = result.getContent().get(0);
        assertThat(resp.productId()).isEqualTo(productId);
        assertThat(resp.imageUrls()).hasSize(1);
    }

    /* ==============================
     *  리뷰 삭제(deleteReview)
     * ============================== */

    @Test
    @DisplayName("리뷰 삭제 성공 - 본인 리뷰")
    void deleteReview_success() {
        // given
        Product product = Mockito.mock(Product.class);
        when(product.getId()).thenReturn(productId);

        Review review = Review.createReview("내용", (short) 5, memberId, product, orderDetailId);

        ReviewImage image = ReviewImage.create("url", review);

        try (MockedStatic<MemberAuthContext> mockedStatic =
                Mockito.mockStatic(MemberAuthContext.class)) {

            mockedStatic.when(MemberAuthContext::getMemberId).thenReturn(memberId);

            when(reviewRepository.findById(any(UUID.class))).thenReturn(Optional.of(review));
            when(reviewImageRepository.findAllByReview(review)).thenReturn(List.of(image));
            when(reviewRepository.findAverageByProductId(any())).thenReturn(0.0);
            when(reviewRepository.findCountByProductId(any())).thenReturn(0);

            // when
            reviewService.deleteReview(UUID.randomUUID());

            // then
            verify(reviewRepository).findById(any(UUID.class));
            verify(reviewImageRepository).findAllByReview(any(Review.class));
            verify(productRepository).save(any(Product.class));
        }
    }

    @Test
    @DisplayName("리뷰 삭제 실패 - 본인 리뷰 아님")
    void deleteReview_fail_unauthorized() {
        // given
        Product product = Mockito.mock(Product.class);
        Review review = Review.createReview("내용", (short) 5, 999L, product, orderDetailId);

        try (MockedStatic<MemberAuthContext> mockedStatic =
                Mockito.mockStatic(MemberAuthContext.class)) {

            mockedStatic.when(MemberAuthContext::getMemberId).thenReturn(memberId);

            when(reviewRepository.findById(any(UUID.class))).thenReturn(Optional.of(review));

            // when & then
            CommonException ex =
                    assertThrows(
                            CommonException.class,
                            () -> reviewService.deleteReview(UUID.randomUUID()));

            assertThat(ex.getErrorCode()).isEqualTo(ReviewErrorCode.REVIEW_UNAUTHORIZED);
        }
    }
}
