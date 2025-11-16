package com.irum.productservice.domain.cart.controller;

import com.irum.productservice.domain.cart.dto.request.CartCreateRequest;
import com.irum.productservice.domain.cart.dto.request.CartUpdateRequest;
import com.irum.productservice.domain.cart.dto.response.CartResponse;
import com.irum.productservice.domain.cart.service.CartService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/carts", produces = "application/json")
@RequiredArgsConstructor
@Validated
public class CartController {

    private final CartService cartService;

    @PostMapping(consumes = "application/json")
    public ResponseEntity<CartResponse> createCart(@RequestBody @Valid CartCreateRequest request) {
        CartResponse response = cartService.createCartWithResponse(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<CartResponse>> getCartList() {
        List<CartResponse> responses = cartService.getCartListByMember();
        return ResponseEntity.ok(responses);
    }

    @PatchMapping(value = "/{cartId}", consumes = "application/json")
    public ResponseEntity<Void> updateCart(
            @PathVariable UUID cartId, @RequestBody @Valid CartUpdateRequest request) {
        cartService.updateCart(cartId, request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{cartId}")
    public ResponseEntity<Void> deleteCart(@PathVariable UUID cartId) {
        cartService.deleteCart(cartId);
        return ResponseEntity.noContent().build();
    }
}
