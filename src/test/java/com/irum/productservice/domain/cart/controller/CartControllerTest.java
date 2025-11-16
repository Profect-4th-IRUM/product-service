package com.irum.productservice.domain.cart.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.irum.productservice.domain.cart.domain.entity.CartRedis;
import com.irum.productservice.domain.cart.dto.request.CartCreateRequest;
import com.irum.productservice.domain.cart.dto.request.CartUpdateRequest;
import com.irum.productservice.domain.cart.dto.response.CartResponse;
import com.irum.productservice.domain.cart.service.CartService;
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
import org.springframework.http.MediaType;
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CartController.class)
@AutoConfigureRestDocs
class CartControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private CartService cartService;
    @Autowired private ObjectMapper objectMapper;

    private final UUID mockCartId = UUID.randomUUID();
    private final UUID mockOptionValueId = UUID.randomUUID();

    @TestConfiguration
    static class TestConfig {
        @Bean
        public CartService cartService() {
            return Mockito.mock(CartService.class);
        }
    }

    @Test
    @DisplayName("장바구니 추가 API (CUSTOMER)")
    void createCartApiTest() throws Exception {
        // given
        CartCreateRequest request = new CartCreateRequest(mockOptionValueId, 2);
        String requestJson = objectMapper.writeValueAsString(request);

        CartResponse response =
                CartResponse.builder()
                        .cartId(mockCartId)
                        .optionValueId(mockOptionValueId)
                        .productName("테스트 상품")
                        .optionValueName("옵션A")
                        .imageUrl("https://example.com/image.jpg")
                        .quantity(2)
                        .basePrice(10_000)
                        .extraPrice(500)
                        .discountAmount(0) // 할인 0원 가정
                        .unitPrice(10_500) // 10,000 + 500 - 0
                        .lineTotal(21_000) // 10,500 * 2
                        .stockQuantity(5) // 재고 5개 가정
                        .build();

        // CartService는 createCartWithResponse 를 사용한다고 가정
        Mockito.when(cartService.createCartWithResponse(any(CartCreateRequest.class)))
                .thenReturn(response);

        // when & then
        mockMvc.perform(
                        RestDocumentationRequestBuilders.post("/carts")
                                .with(csrf())
                                .with(user("1").roles("CUSTOMER"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson))
                .andExpect(status().isCreated())
                .andDo(
                        document(
                                "cart-create",
                                requestFields(
                                        fieldWithPath("optionValueId").description("옵션 값 ID"),
                                        fieldWithPath("quantity").description("상품 수량")),
                                responseFields(
                                        fieldWithPath("success").description("true"),
                                        fieldWithPath("status").description("201"),
                                        fieldWithPath("timestamp").description("응답 시간"),
                                        fieldWithPath("data.cartId").description("장바구니 ID"),
                                        fieldWithPath("data.optionValueId").description("옵션 값 ID"),
                                        fieldWithPath("data.productName").description("상품 이름"),
                                        fieldWithPath("data.optionValueName").description("옵션 이름"),
                                        fieldWithPath("data.imageUrl").description("대표 이미지 URL"),
                                        fieldWithPath("data.quantity").description("수량"),
                                        fieldWithPath("data.basePrice").description("상품 기본가"),
                                        fieldWithPath("data.extraPrice").description("옵션 추가금"),
                                        fieldWithPath("data.discountAmount").description("할인 금액"),
                                        fieldWithPath("data.unitPrice").description("단가"),
                                        fieldWithPath("data.lineTotal").description("총 금액"),
                                        fieldWithPath("data.stockQuantity")
                                                .description("옵션 재고 수량"))));
    }

    @Test
    @DisplayName("장바구니 목록 조회 API (CUSTOMER)")
    void getCartListApiTest() throws Exception {
        // given
        CartResponse response =
                CartResponse.builder()
                        .cartId(mockCartId)
                        .optionValueId(mockOptionValueId)
                        .productName("테스트 상품")
                        .optionValueName("옵션A")
                        .imageUrl("https://example.com/image.jpg")
                        .quantity(2)
                        .basePrice(10_000)
                        .extraPrice(500)
                        .discountAmount(0)
                        .unitPrice(10_500)
                        .lineTotal(21_000)
                        .stockQuantity(5)
                        .build();

        Mockito.when(cartService.getCartListByMember()).thenReturn(List.of(response));

        // when & then
        mockMvc.perform(
                        RestDocumentationRequestBuilders.get("/carts")
                                .with(csrf().asHeader())
                                .with(user("1").roles("CUSTOMER")))
                .andExpect(status().isOk())
                .andDo(
                        document(
                                "cart-get-list",
                                responseFields(
                                        fieldWithPath("success").description("true"),
                                        fieldWithPath("status").description("200"),
                                        fieldWithPath("timestamp").description("응답 시간"),
                                        fieldWithPath("data[].cartId").description("장바구니 ID"),
                                        fieldWithPath("data[].optionValueId")
                                                .description("옵션 값 ID"),
                                        fieldWithPath("data[].productName").description("상품 이름"),
                                        fieldWithPath("data[].optionValueName")
                                                .description("옵션 이름"),
                                        fieldWithPath("data[].imageUrl").description("대표 이미지 URL"),
                                        fieldWithPath("data[].quantity").description("수량"),
                                        fieldWithPath("data[].basePrice").description("상품 기본가"),
                                        fieldWithPath("data[].extraPrice").description("옵션 추가금"),
                                        fieldWithPath("data[].discountAmount").description("할인 금액"),
                                        fieldWithPath("data[].unitPrice").description("단가"),
                                        fieldWithPath("data[].lineTotal").description("총 금액"),
                                        fieldWithPath("data[].stockQuantity")
                                                .description("옵션 재고 수량"))));
    }

    @Test
    @DisplayName("장바구니 수정 API (CUSTOMER)")
    void updateCartApiTest() throws Exception {
        // given
        CartUpdateRequest request = new CartUpdateRequest(5);
        String requestJson = objectMapper.writeValueAsString(request);

        // updateCart는 CartRedis를 반환하므로, 목 객체를 리턴하도록 설정
        Mockito.when(cartService.updateCart(eq(mockCartId), any(CartUpdateRequest.class)))
                .thenReturn(Mockito.mock(CartRedis.class));

        // when & then
        mockMvc.perform(
                        RestDocumentationRequestBuilders.patch("/carts/{cartId}", mockCartId)
                                .with(csrf())
                                .with(user("1").roles("CUSTOMER"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson))
                .andExpect(status().isNoContent())
                .andDo(
                        document(
                                "cart-update",
                                pathParameters(
                                        parameterWithName("cartId").description("수정할 장바구니 ID")),
                                requestFields(fieldWithPath("quantity").description("변경할 수량"))));
    }

    @Test
    @DisplayName("장바구니 삭제 API (CUSTOMER)")
    void deleteCartApiTest() throws Exception {
        // given
        Mockito.doNothing().when(cartService).deleteCart(eq(mockCartId));

        // when & then
        mockMvc.perform(
                        RestDocumentationRequestBuilders.delete("/carts/{cartId}", mockCartId)
                                .with(csrf())
                                .with(user("1").roles("CUSTOMER")))
                .andExpect(status().isNoContent())
                .andDo(
                        document(
                                "cart-delete",
                                pathParameters(
                                        parameterWithName("cartId").description("삭제할 장바구니 ID"))));
    }
}
