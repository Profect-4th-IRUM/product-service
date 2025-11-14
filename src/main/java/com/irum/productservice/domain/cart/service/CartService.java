package com.irum.productservice.domain.cart.service;

import com.irum.global.advice.exception.CommonException;
import com.irum.productservice.domain.cart.domain.entity.CartRedis;
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
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartService {

    private final CartRedisRepository cartRedisRepository;
    private final ProductOptionValueRepository productOptionValueRepository;
    private final DiscountRepository discountRepository;
    private final CartMapper cartMapper;
    private final MemberUtil memberUtil;

    private static final long TTL_SECONDS = 30L;

    /** 장바구니 아이템 추가 */
    public CartRedis createCart(CartCreateRequest request) {
        Long memberId = memberUtil.getCurrentMember().memberId();

        if (request.quantity() <= 0) {
            throw new CommonException(CartErrorCode.INVALID_QUANTITY);
        }

        ProductOptionValue optionValue =
                productOptionValueRepository
                        .findById(request.optionValueId())
                        .orElseThrow(
                                () -> new CommonException(ProductErrorCode.OPTION_VALUE_NOT_FOUND));

        try {
            // 1) 먼저 내 장바구니 전체를 Redis에서 가져온 다음
            List<CartRedis> myCarts = cartRedisRepository.findByMemberId(memberId);

            // 2) optionValueId 같은 게 있는지 자바에서 필터링
            CartRedis existing =
                    myCarts.stream()
                            .filter(c -> c.getOptionValueId().equals(optionValue.getId()))
                            .findFirst()
                            .orElse(null);

            if (existing != null) {
                existing.updateQuantity(existing.getQuantity() + request.quantity());
                return cartRedisRepository.save(existing);
            } else {
                UUID cartId = UUID.randomUUID();
                CartRedis newCart =
                        CartRedis.of(
                                memberId,
                                cartId,
                                optionValue.getId(),
                                request.quantity(),
                                TTL_SECONDS);
                return cartRedisRepository.save(newCart);
            }

        } catch (Exception e) {
            log.error(
                    "[CartService] Redis error in createCart (memberId={}, optionValueId={})",
                    memberId, request.optionValueId(), e);
            throw e;
        }
    }

    /** 장바구니 아이템 추가 + DTO 반환 */
    public CartResponse createCartWithResponse(CartCreateRequest request) {
        CartRedis cart = createCart(request);
        ProductOptionValue optionValue =
                productOptionValueRepository
                        .findById(request.optionValueId())
                        .orElseThrow(
                                () -> new CommonException(ProductErrorCode.OPTION_VALUE_NOT_FOUND));

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

        // 내 장바구니 전체에서 cartId로 찾기
        CartRedis cart =
                cartRedisRepository.findByMemberId(memberId).stream()
                        .filter(c -> c.getCartId().equals(cartId))
                        .findFirst()
                        .orElseThrow(() -> new CommonException(CartErrorCode.CART_EXPIRED));

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

        List<CartRedis> carts = cartRedisRepository.findByMemberId(memberId);

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
                                                                            .OPTION_VALUE_NOT_FOUND));

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

        // 내 장바구니에서 cartId로 찾아서, Redis key(id) 기준으로 삭제
        cartRedisRepository.findByMemberId(memberId).stream()
                .filter(c -> c.getCartId().equals(cartId))
                .findFirst()
                .ifPresent(cart -> cartRedisRepository.deleteById(cart.getId()));
    }
}
