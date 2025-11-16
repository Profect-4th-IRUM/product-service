package com.irum.productservice.domain.cart.service;

import com.irum.global.advice.exception.CommonException;
import com.irum.openfeign.member.dto.response.MemberDto;
import com.irum.productservice.domain.cart.domain.entity.CartRedis;
import com.irum.productservice.domain.cart.domain.repository.CartRedisRepository;
import com.irum.productservice.domain.cart.dto.request.CartCreateRequest;
import com.irum.productservice.domain.cart.dto.request.CartUpdateRequest;
import com.irum.productservice.domain.cart.dto.response.CartResponse;
import com.irum.productservice.domain.cart.mapper.CartMapper;
import com.irum.productservice.domain.discount.domain.entity.Discount;
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

    private static final long TTL_SECONDS = 604800L;

    private Long requireCurrentMemberId() {
        MemberDto member = memberUtil.getCurrentMember();
        if (member == null) throw new CommonException(CartErrorCode.CART_UNAUTHORIZED);
        return member.memberId();
    }

    /** 장바구니 추가 */
    public CartRedis createCart(CartCreateRequest request) {
        Long memberId = requireCurrentMemberId();

        if (request.quantity() <= 0) throw new CommonException(CartErrorCode.INVALID_QUANTITY);

        ProductOptionValue optionValue =
                productOptionValueRepository
                        .findById(request.optionValueId())
                        .orElseThrow(
                                () -> new CommonException(ProductErrorCode.OPTION_VALUE_NOT_FOUND));

        // 이미 같은 옵션이 있는지 확인
        return cartRedisRepository
                .findByMemberIdAndOptionValueId(memberId, optionValue.getId())
                .map(
                        existing -> {
                            existing.updateQuantity(existing.getQuantity() + request.quantity());
                            existing.refreshTtl(TTL_SECONDS);
                            return cartRedisRepository.save(existing);
                        })
                .orElseGet(
                        () -> {
                            UUID cartId = UUID.randomUUID();
                            CartRedis newCart =
                                    CartRedis.of(
                                            memberId,
                                            cartId,
                                            optionValue.getId(),
                                            request.quantity(),
                                            TTL_SECONDS);
                            return cartRedisRepository.save(newCart);
                        });
    }

    /** DTO 응답 포함 */
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
                        .map(Discount::getAmount)
                        .orElse(0);

        return cartMapper.toResponse(cart, optionValue, discount);
    }

    /** 수량 수정 */
    public CartRedis updateCart(UUID cartId, CartUpdateRequest request) {
        Long memberId = requireCurrentMemberId();

        if (request.quantity() <= 0) throw new CommonException(CartErrorCode.INVALID_QUANTITY);

        CartRedis cart =
                cartRedisRepository
                        .findByMemberIdAndCartId(memberId, cartId)
                        .orElseThrow(() -> new CommonException(CartErrorCode.CART_EXPIRED));

        cart.updateQuantity(request.quantity());
        cart.refreshTtl(TTL_SECONDS);

        return cartRedisRepository.save(cart);
    }

    /** 장바구니 목록 조회 */
    public List<CartResponse> getCartListByMember() {
        Long memberId = requireCurrentMemberId();

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

                            int discount =
                                    discountRepository
                                            .findByProductId(
                                                    optionValue
                                                            .getOptionGroup()
                                                            .getProduct()
                                                            .getId())
                                            .map(Discount::getAmount)
                                            .orElse(0);

                            return cartMapper.toResponse(cart, optionValue, discount);
                        })
                .collect(Collectors.toList());
    }

    /** 장바구니 삭제 */
    public void deleteCart(UUID cartId) {
        Long memberId = requireCurrentMemberId();

        cartRedisRepository
                .findByMemberIdAndCartId(memberId, cartId)
                .ifPresent(
                        cart -> {
                            cartRedisRepository.deleteById(cart.getId());
                            log.info(
                                    "[CartService] delete cart: cartId={}, memberId={}",
                                    cart.getCartId(),
                                    cart.getMemberId());
                        });
    }
}
