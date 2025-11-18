package com.irum.productservice.domain.cart.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.irum.productservice.domain.cart.domain.entity.CartItem;
import com.irum.productservice.domain.cart.dto.request.CartCreateRequest;
import com.irum.productservice.domain.cart.dto.request.CartUpdateRequest;
import com.irum.productservice.domain.cart.dto.response.CartResponse;
import com.irum.productservice.domain.cart.service.CartService;
import com.irum.productservice.global.config.TestConfig;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CartController.class)
@AutoConfigureRestDocs
@Import(TestConfig.class)
class CartControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private CartService cartService;
    @Autowired private ObjectMapper objectMapper;

    private final String mockCartItemId = UUID.randomUUID().toString();
    private final UUID mockOptionValueId = UUID.randomUUID();

    @Test
    @DisplayName("장바구니 추가 API (CUSTOMER)")
    void createCartApiTest() throws Exception {
        // given
        CartCreateRequest request = new CartCreateRequest(mockOptionValueId, 2);
        String requestJson = objectMapper.writeValueAsString(request);

        CartResponse response =
                CartResponse.builder()
                        .cartItemId(mockCartItemId)
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

        when(cartService.createCartWithResponse(any(CartCreateRequest.class))).thenReturn(response);

        // when & then
        mockMvc.perform(
                        post("/carts")
                                .with(csrf())
                                .with(user("1").roles("CUSTOMER"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(HttpStatus.CREATED.value()))
                .andExpect(jsonPath("$.data.cartItemId").value(mockCartItemId))
                .andExpect(jsonPath("$.data.optionValueId").value(mockOptionValueId.toString()))
                .andExpect(jsonPath("$.data.productName").value("테스트 상품"))
                .andDo(
                        document(
                                "cart-create",
                                requestFields(
                                        fieldWithPath("optionValueId").description("옵션 값 ID"),
                                        fieldWithPath("quantity").description("상품 수량")),
                                responseFields(
                                        fieldWithPath("success").description("true"),
                                        fieldWithPath("status").description("HTTP 상태 코드"),
                                        fieldWithPath("timestamp").description("응답 생성 시간"),
                                        fieldWithPath("data.cartItemId").description("장바구니 아이템 ID"),
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
                        .cartItemId(mockCartItemId)
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

        when(cartService.getCartListByMember()).thenReturn(List.of(response));

        // when & then
        mockMvc.perform(get("/carts").with(csrf().asHeader()).with(user("1").roles("CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(HttpStatus.OK.value()))
                .andExpect(jsonPath("$.data[0].cartItemId").value(mockCartItemId))
                .andDo(
                        document(
                                "cart-get-list",
                                responseFields(
                                        fieldWithPath("success").description("true"),
                                        fieldWithPath("status").description("HTTP 상태 코드"),
                                        fieldWithPath("timestamp").description("응답 생성 시간"),
                                        fieldWithPath("data[].cartItemId")
                                                .description("장바구니 아이템 ID"),
                                        fieldWithPath("data[].optionValueId")
                                                .description("옵션 값 ID"),
                                        fieldWithPath("data[].productName").description("상품 이름"),
                                        fieldWithPath("data[].optionValueName")
                                                .description("옵션 이름"),
                                        fieldWithPath("data[].imageUrl").description("대표 이미지 URL"),
                                        fieldWithPath("data[].quantity").description("수량"),
                                        fieldWithPath("data[].basePrice").description("상품 기본가"),
                                        fieldWithPath("data[].extraPrice").description("옵션 추가금"),
                                        fieldWithPath("data[].discountAmount").description("홀인 금액"),
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

        when(cartService.updateCart(eq(mockCartItemId), any(CartUpdateRequest.class)))
                .thenReturn((CartItem) Mockito.mock(Object.class));

        // when & then
        mockMvc.perform(
                        patch("/carts/{cartItemId}", mockCartItemId)
                                .with(csrf())
                                .with(user("1").roles("CUSTOMER"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson))
                .andExpect(status().isNoContent())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(HttpStatus.NO_CONTENT.value()))
                .andDo(
                        document(
                                "cart-update",
                                pathParameters(
                                        parameterWithName("cartItemId")
                                                .description("수정할 장바구니 아이템 ID")),
                                requestFields(fieldWithPath("quantity").description("변경할 수량")),
                                responseFields(
                                        fieldWithPath("success").description("true"),
                                        fieldWithPath("status").description("HTTP 상태 코드"),
                                        fieldWithPath("timestamp").description("응답 생성 시간"),
                                        fieldWithPath("data").description("null 또는 응답 데이터 없음"))));
    }

    @Test
    @DisplayName("장바구니 삭제 API (CUSTOMER)")
    void deleteCartApiTest() throws Exception {
        doNothing().when(cartService).deleteCart(eq(mockCartItemId));

        mockMvc.perform(
                        delete("/carts/{cartItemId}", mockCartItemId)
                                .with(csrf())
                                .with(user("1").roles("CUSTOMER")))
                .andExpect(status().isNoContent())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(HttpStatus.NO_CONTENT.value()))
                .andDo(
                        document(
                                "cart-delete",
                                pathParameters(
                                        parameterWithName("cartItemId")
                                                .description("삭제할 장바구니 아이템 ID")),
                                responseFields(
                                        fieldWithPath("success").description("true"),
                                        fieldWithPath("status").description("HTTP 상태 코드"),
                                        fieldWithPath("timestamp").description("응답 생성 시간"),
                                        fieldWithPath("data").description("null 또는 응답 데이터 없음"))));
    }
}
