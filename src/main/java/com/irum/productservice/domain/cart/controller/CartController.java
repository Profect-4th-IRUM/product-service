package com.irum.productservice.domain.cart.controller;

import com.irum.productservice.domain.cart.dto.request.CartCreateRequest;
import com.irum.productservice.domain.cart.dto.request.CartUpdateRequest;
import com.irum.productservice.domain.cart.dto.response.CartResponse;
import com.irum.productservice.domain.cart.service.CartService;
import jakarta.validation.Valid;
import java.util.List;
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

    @PatchMapping(value = "/{cartItemId}", consumes = "application/json")
    public ResponseEntity<Void> updateCart(
            @PathVariable String cartItemId, @RequestBody @Valid CartUpdateRequest request) {
        cartService.updateCart(cartItemId, request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{cartItemId}")
    public ResponseEntity<Void> deleteCart(@PathVariable String cartItemId) {
        cartService.deleteCart(cartItemId);
        return ResponseEntity.noContent().build();
    }
}
