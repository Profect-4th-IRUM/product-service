package com.irum.productservice.domain.review.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.irum.productservice.domain.review.dto.request.ReviewCreateRequest;
import com.irum.productservice.domain.review.dto.request.ReviewUpdateRequest;
import com.irum.productservice.domain.review.dto.response.ReviewResponse;
import com.irum.productservice.domain.review.service.ReviewService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ReviewController.class)
@AutoConfigureRestDocs
class ReviewControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private ReviewService reviewService;

    private final UUID mockReviewId = UUID.randomUUID();
    private final UUID mockProductId = UUID.randomUUID();
    private final UUID mockOrderDetailId = UUID.randomUUID();

    @TestConfiguration
    static class TestConfig {
        @Bean
        public ReviewService reviewService() {
            return Mockito.mock(ReviewService.class);
        }
    }

    private ReviewResponse createMockReviewResponse() {
        return new ReviewResponse(
                mockReviewId,
                mockProductId,
                1L,
                "맛있어요!",
                (short) 5,
                List.of("https://example.com/review1.jpg"));
    }

    @Test
    @DisplayName("리뷰 생성 API (CUSTOMER)")
    void createReviewApiTest() throws Exception {
        // given
        ReviewCreateRequest request =
                new ReviewCreateRequest(
                        mockOrderDetailId,
                        mockProductId,
                        "도시락 너무 맛있어요!",
                        (short) 5,
                        List.of("https://example.com/review1.jpg"));

        String requestJson = objectMapper.writeValueAsString(request);

        ReviewResponse response = createMockReviewResponse();

        Mockito.when(reviewService.createReview(any(ReviewCreateRequest.class)))
                .thenReturn(response);

        // when & then
        mockMvc.perform(
                        RestDocumentationRequestBuilders.post("/reviews")
                                .with(csrf())
                                .with(user("1").roles("CUSTOMER"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson))
                .andExpect(status().isCreated())
                .andDo(
                        document(
                                "review-create",
                                requestFields(
                                        fieldWithPath("orderDetailId").description("주문 상세 ID"),
                                        fieldWithPath("productId").description("상품 ID"),
                                        fieldWithPath("content").description("리뷰 내용"),
                                        fieldWithPath("rate").description("평점 (1~5)"),
                                        fieldWithPath("imageUrls")
                                                .description("리뷰 이미지 URL 목록 (최대 5개)")),
                                responseFields(
                                        fieldWithPath("reviewId").description("리뷰 ID"),
                                        fieldWithPath("productId").description("상품 ID"),
                                        fieldWithPath("memberId").description("회원 ID"),
                                        fieldWithPath("content").description("리뷰 내용"),
                                        fieldWithPath("rate").description("평점"),
                                        fieldWithPath("imageUrls[]")
                                                .description("리뷰 이미지 URL 목록"))));
    }

    @Test
    @DisplayName("리뷰 수정 API (CUSTOMER)")
    void updateReviewApiTest() throws Exception {
        // given
        ReviewUpdateRequest request =
                new ReviewUpdateRequest(
                        "리뷰 내용 수정", (short) 4, List.of("https://example.com/review2.jpg"));
        String requestJson = objectMapper.writeValueAsString(request);

        ReviewResponse response =
                new ReviewResponse(
                        mockReviewId,
                        mockProductId,
                        1L,
                        "리뷰 내용 수정",
                        (short) 4,
                        List.of("https://example.com/review2.jpg"));

        Mockito.when(reviewService.updateReview(eq(mockReviewId), any(ReviewUpdateRequest.class)))
                .thenReturn(response);

        // when & then
        mockMvc.perform(
                        RestDocumentationRequestBuilders.patch("/reviews/{reviewId}", mockReviewId)
                                .with(csrf())
                                .with(user("1").roles("CUSTOMER"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson))
                .andExpect(status().isOk())
                .andDo(
                        document(
                                "review-update",
                                pathParameters(
                                        parameterWithName("reviewId").description("수정할 리뷰 ID")),
                                requestFields(
                                        fieldWithPath("content")
                                                .description("수정할 리뷰 내용 (null이면 변경 없음)")
                                                .optional(),
                                        fieldWithPath("rate")
                                                .description("수정할 평점 (1~5, null이면 변경 없음)")
                                                .optional(),
                                        fieldWithPath("imageUrls")
                                                .description("수정할 리뷰 이미지 URL 목록 (null이면 변경 없음)")
                                                .optional()),
                                responseFields(
                                        fieldWithPath("reviewId").description("리뷰 ID"),
                                        fieldWithPath("productId").description("상품 ID"),
                                        fieldWithPath("memberId").description("회원 ID"),
                                        fieldWithPath("content").description("리뷰 내용"),
                                        fieldWithPath("rate").description("평점"),
                                        fieldWithPath("imageUrls[]")
                                                .description("리뷰 이미지 URL 목록"))));
    }

    @Test
    @DisplayName("내 리뷰 목록 조회 API (CUSTOMER)")
    void getMyReviewsApiTest() throws Exception {
        // given
        ReviewResponse reviewResponse = createMockReviewResponse();
        Page<ReviewResponse> page =
                new PageImpl<>(List.of(reviewResponse), PageRequest.of(0, 10), 1);

        Mockito.when(reviewService.getMyReviews(any())).thenReturn(page);

        // when & then
        mockMvc.perform(
                        RestDocumentationRequestBuilders.get("/reviews/me")
                                .param("page", "0")
                                .param("size", "10")
                                .with(csrf().asHeader())
                                .with(user("1").roles("CUSTOMER")))
                .andExpect(status().isOk())
                .andDo(
                        document(
                                "review-get-my-reviews",
                                responseFields(
                                        fieldWithPath("reviewList[].reviewId").description("리뷰 ID"),
                                        fieldWithPath("reviewList[].productId")
                                                .description("상품 ID"),
                                        fieldWithPath("reviewList[].memberId").description("회원 ID"),
                                        fieldWithPath("reviewList[].content").description("리뷰 내용"),
                                        fieldWithPath("reviewList[].rate").description("평점"),
                                        fieldWithPath("reviewList[].imageUrls[]")
                                                .description("리뷰 이미지 URL 목록"),
                                        fieldWithPath("pageInfo.pageNumber")
                                                .description("현재 페이지 번호(0부터 시작)"),
                                        fieldWithPath("pageInfo.pageSize").description("페이지 크기"),
                                        fieldWithPath("pageInfo.totalElements")
                                                .description("전체 요소 수"),
                                        fieldWithPath("pageInfo.totalPages")
                                                .description("전체 페이지 수"),
                                        fieldWithPath("pageInfo.last").description("마지막 페이지 여부"))));
    }

    @Test
    @DisplayName("상품별 리뷰 목록 조회 API")
    void getProductReviewsApiTest() throws Exception {
        // given
        ReviewResponse reviewResponse = createMockReviewResponse();
        Page<ReviewResponse> page =
                new PageImpl<>(List.of(reviewResponse), PageRequest.of(0, 10), 1);

        Mockito.when(reviewService.getProductReviews(eq(mockProductId), any())).thenReturn(page);

        // when & then
        mockMvc.perform(
                        RestDocumentationRequestBuilders.get(
                                        "/reviews/products/{productId}", mockProductId)
                                .param("page", "0")
                                .param("size", "10")
                                .with(csrf().asHeader())
                                .with(user("1").roles("CUSTOMER")))
                .andExpect(status().isOk())
                .andDo(
                        document(
                                "review-get-product-reviews",
                                pathParameters(parameterWithName("productId").description("상품 ID")),
                                responseFields(
                                        fieldWithPath("reviewList[].reviewId").description("리뷰 ID"),
                                        fieldWithPath("reviewList[].productId")
                                                .description("상품 ID"),
                                        fieldWithPath("reviewList[].memberId").description("회원 ID"),
                                        fieldWithPath("reviewList[].content").description("리뷰 내용"),
                                        fieldWithPath("reviewList[].rate").description("평점"),
                                        fieldWithPath("reviewList[].imageUrls[]")
                                                .description("리뷰 이미지 URL 목록"),
                                        fieldWithPath("pageInfo.pageNumber")
                                                .description("현재 페이지 번호(0부터 시작)"),
                                        fieldWithPath("pageInfo.pageSize").description("페이지 크기"),
                                        fieldWithPath("pageInfo.totalElements")
                                                .description("전체 요소 수"),
                                        fieldWithPath("pageInfo.totalPages")
                                                .description("전체 페이지 수"),
                                        fieldWithPath("pageInfo.last").description("마지막 페이지 여부"))));
    }

    @Test
    @DisplayName("리뷰 삭제 API (CUSTOMER)")
    void deleteReviewApiTest() throws Exception {
        // given
        Mockito.doNothing().when(reviewService).deleteReview(eq(mockReviewId));

        // when & then
        mockMvc.perform(
                        RestDocumentationRequestBuilders.delete("/reviews/{reviewId}", mockReviewId)
                                .with(csrf())
                                .with(user("1").roles("CUSTOMER")))
                .andExpect(status().isNoContent())
                .andDo(
                        document(
                                "review-delete",
                                pathParameters(
                                        parameterWithName("reviewId").description("삭제할 리뷰 ID"))));
    }
}
