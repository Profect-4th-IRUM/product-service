//package com.irum.productservice.domain.cart.controller;
//
//import com.irum.productservice.domain.cart.dto.request.CartCreateRequest;
//import com.irum.productservice.domain.cart.dto.request.CartUpdateRequest;
//import com.irum.productservice.domain.cart.dto.response.CartResponse;
//import com.irum.productservice.domain.cart.service.CartService;
//import jakarta.validation.Valid;
//import java.util.List;
//import java.util.UUID;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//@RestController
//@RequestMapping("/carts")
//@RequiredArgsConstructor
//@Slf4j
//public class CartController {
//
//    private final CartService cartService;
//
//    @PostMapping
//    public ResponseEntity<CartResponse> createCart(@Valid @RequestBody CartCreateRequest request) {
//        log.info(
//                "장바구니 추가 요청: optionValueId={}, quantity={}",
//                request.optionValueId(),
//                request.quantity());
//        CartResponse response = cartService.createCart(request);
//        return ResponseEntity.status(HttpStatus.CREATED).body(response);
//    }
//
//    @PatchMapping("/{cartId}")
//    public ResponseEntity<Void> updateCart(
//            @PathVariable UUID cartId, @Valid @RequestBody CartUpdateRequest request) {
//        log.info("장바구니 수정 요청: cartId={}, quantity={}", cartId, request.quantity());
//        cartService.updateCart(cartId, request);
//        return ResponseEntity.noContent().build();
//    }
//
//    @GetMapping
//    public ResponseEntity<List<CartResponse>> getMyCartList() {
//        log.info("장바구니 목록 조회 요청");
//        List<CartResponse> responses = cartService.getCartListByMember();
//        log.info("장바구니 조회 결과: {}개 항목", responses.size());
//        return ResponseEntity.ok(responses);
//    }
//
//    @DeleteMapping("/{cartId}")
//    public ResponseEntity<Void> deleteCart(@PathVariable UUID cartId) {
//        log.info("장바구니 삭제 요청: cartId={}", cartId);
//        cartService.deleteCart(cartId);
//        return ResponseEntity.noContent().build();
//    }
//}
