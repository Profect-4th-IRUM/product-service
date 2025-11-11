package com.irum.productservice.domain.cart.service;

import com.irum.global.advice.exception.CommonException;
import com.irum.productservice.domain.cart.domain.model.CartRedis;
import com.irum.productservice.domain.cart.domain.repository.CartRedisRepository;
import com.irum.productservice.domain.cart.dto.request.CartCreateRequest;
import com.irum.productservice.domain.cart.dto.request.CartUpdateRequest;
import com.irum.productservice.domain.cart.dto.response.CartResponse;
import com.irum.productservice.domain.cart.mapper.CartMapper;
import com.irum.productservice.domain.discount.domain.repository.DiscountRepository;
import com.irum.productservice.domain.product.domain.entity.ProductOptionValue;
import com.irum.productservice.domain.product.domain.repository.ProductOptionValueRepository;
import com.irum.productservice.global.exception.errorcode.CartErrorCode;
import com.irum.productservice.global.exception.errorcode.ProductErrorCode;
import com.irum.productservice.global.util.MemberUtil;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRedisRepository cartRedisRepository;
    private final ProductOptionValueRepository productOptionValueRepository;
    private final DiscountRepository discountRepository;
    private final CartMapper cartMapper;
    private final MemberUtil memberUtil;

    /** 장바구니 아이템 추가 */
    public CartRedis createCart(CartCreateRequest request) {
        Long memberId = memberUtil.getCurrentMember().memberId();

        if (request.quantity() <= 0) {
            throw new CommonException(CartErrorCode.INVALID_QUANTITY);
        }

        ProductOptionValue optionValue =
                productOptionValueRepository
                        .findById(request.optionValueId())
                        .orElseThrow(() -> new CommonException(ProductErrorCode.PRODUCT_NOT_FOUND));

        CartRedis cart =
                new CartRedis(UUID.randomUUID(), memberId, optionValue.getId(), request.quantity());
        cartRedisRepository.save(cart);
        return cart;
    }

    /** 장바구니 아이템 추가 + DTO 반환 */
    public CartResponse createCartWithResponse(CartCreateRequest request) {
        CartRedis cart = createCart(request);
        ProductOptionValue optionValue =
                productOptionValueRepository
                        .findById(request.optionValueId())
                        .orElseThrow(() -> new CommonException(ProductErrorCode.PRODUCT_NOT_FOUND));

        int discount =
                discountRepository
                        .findByProductId(optionValue.getOptionGroup().getProduct().getId())
                        .map(d -> d.getAmount())
                        .orElse(0);

        return cartMapper.toResponse(cart, optionValue, discount);
    }

    /** 장바구니 아이템 수정 */
    public CartRedis updateCart(UUID cartId, CartUpdateRequest request) {
        Long memberId = memberUtil.getCurrentMember().memberId();
        CartRedis cart = cartRedisRepository.findById(memberId, cartId);

        if (cart == null) {
            throw new CommonException(CartErrorCode.CART_EXPIRED);
        }

        if (request.quantity() <= 0) {
            throw new CommonException(CartErrorCode.INVALID_QUANTITY);
        }

        if (!cart.getMemberId().equals(memberId)) {
            throw new CommonException(CartErrorCode.CART_UNAUTHORIZED);
        }

        cart.updateQuantity(request.quantity());
        cartRedisRepository.save(cart);

        return cart;
    }

    /** 내 장바구니 아이템 목록 조회 */
    public List<CartResponse> getCartListByMember() {
        Long memberId = memberUtil.getCurrentMember().memberId();
        List<CartRedis> carts = cartRedisRepository.findAll(memberId);

        return carts.stream()
                .map(
                        cart -> {
                            ProductOptionValue optionValue =
                                    productOptionValueRepository
                                            .findById(cart.getOptionValueId())
                                            .orElseThrow(
                                                    () ->
                                                            new CommonException(
                                                                    ProductErrorCode
                                                                            .PRODUCT_NOT_FOUND));

                            var product = optionValue.getOptionGroup().getProduct();
                            int discount =
                                    discountRepository
                                            .findByProductId(product.getId())
                                            .map(d -> d.getAmount())
                                            .orElse(0);

                            return cartMapper.toResponse(cart, optionValue, discount);
                        })
                .collect(Collectors.toList());
    }

    /** 장바구니 아이템 삭제 */
    public void deleteCart(UUID cartId) {
        Long memberId = memberUtil.getCurrentMember().memberId();
        cartRedisRepository.delete(memberId, cartId);
    }
}
