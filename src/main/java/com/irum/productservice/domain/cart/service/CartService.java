package com.irum.productservice.domain.cart.service;

import com.irum.global.advice.exception.CommonException;
import com.irum.openfeign.member.dto.response.MemberDto;
import com.irum.productservice.domain.cart.domain.entity.CartItem;
import com.irum.productservice.domain.cart.domain.repository.CartItemRepository;
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
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartService {

    private final CartItemRepository cartItemRepository;
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
    public CartItem createCart(CartCreateRequest request) {
        Long memberId = requireCurrentMemberId();

        if (request.quantity() <= 0) throw new CommonException(CartErrorCode.INVALID_QUANTITY);

        ProductOptionValue optionValue =
                productOptionValueRepository
                        .findById(request.optionValueId())
                        .orElseThrow(
                                () -> new CommonException(ProductErrorCode.OPTION_VALUE_NOT_FOUND));

        // 이미 같은 옵션이 있는지 확인
        return cartItemRepository
                .findByMemberIdAndOptionValueId(memberId, optionValue.getId())
                .map(
                        existing -> {
                            existing.updateQuantity(existing.getQuantity() + request.quantity());
                            existing.refreshTtl(TTL_SECONDS);
                            return cartItemRepository.save(existing);
                        })
                .orElseGet(
                        () -> {
                            CartItem newCartItem =
                                    CartItem.of(
                                            memberId,
                                            optionValue.getId(),
                                            request.quantity(),
                                            TTL_SECONDS);
                            return cartItemRepository.save(newCartItem);
                        });
    }

    /** DTO 응답 포함 */
    public CartResponse createCartWithResponse(CartCreateRequest request) {
        CartItem cartItem = createCart(request);

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

        return cartMapper.toResponse(cartItem, optionValue, discount);
    }

    /** 수량 수정 */
    public CartItem updateCart(String cartItemId, CartUpdateRequest request) {
        Long memberId = requireCurrentMemberId();

        if (request.quantity() <= 0) throw new CommonException(CartErrorCode.INVALID_QUANTITY);

        CartItem cartItem =
                cartItemRepository
                        .findByMemberIdAndCartItemId(memberId, cartItemId)
                        .orElseThrow(() -> new CommonException(CartErrorCode.CART_EXPIRED));

        cartItem.updateQuantity(request.quantity());
        cartItem.refreshTtl(TTL_SECONDS);

        return cartItemRepository.save(cartItem);
    }

    /** 장바구니 목록 조회 */
    public List<CartResponse> getCartListByMember() {
        Long memberId = requireCurrentMemberId();

        List<CartItem> cartItems = cartItemRepository.findByMemberId(memberId);

        return cartItems.stream()
                .map(
                        cartItem -> {
                            ProductOptionValue optionValue =
                                    productOptionValueRepository
                                            .findById(cartItem.getOptionValueId())
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

                            return cartMapper.toResponse(cartItem, optionValue, discount);
                        })
                .collect(Collectors.toList());
    }

    /** 장바구니 삭제 */
    public void deleteCart(String cartItemId) {
        Long memberId = requireCurrentMemberId();

        cartItemRepository
                .findByMemberIdAndCartItemId(memberId, cartItemId)
                .ifPresent(
                        cartItem -> {
                            cartItemRepository.deleteById(cartItem.getCartItemId());
                            log.info(
                                    "[CartService] delete cartItem: cartItemId={}, memberId={}",
                                    cartItem.getCartItemId(),
                                    cartItem.getMemberId());
                        });
    }
}
