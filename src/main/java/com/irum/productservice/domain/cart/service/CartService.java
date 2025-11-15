package com.irum.productservice.domain.cart.service;

import com.irum.global.advice.exception.CommonException;
import com.irum.openfeign.member.dto.response.MemberDto;
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
import java.util.stream.StreamSupport;
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
        if (member == null) {
            log.warn("[CartService] requireCurrentMemberId -> member is null");
            throw new CommonException(CartErrorCode.CART_UNAUTHORIZED);
        }
        return member.memberId();
    }

    /** Redis findAll → 특정 회원 것만 필터링 */
    private List<CartRedis> findAllForMember(Long memberId) {
        List<CartRedis> list =
                StreamSupport.stream(cartRedisRepository.findAll().spliterator(), false)
                        .filter(c -> c != null && c.getMemberId() != null)
                        .filter(c -> memberId.equals(c.getMemberId()))
                        .collect(Collectors.toList());

        log.info("[CartService] findAllForMember memberId={}, size={}", memberId, list.size());
        return list;
    }

    /** 장바구니 아이템 추가 (같은 옵션이면 quantity 증가) */
    public CartRedis createCart(CartCreateRequest request) {
        Long memberId = requireCurrentMemberId();

        if (request.quantity() <= 0) {
            throw new CommonException(CartErrorCode.INVALID_QUANTITY);
        }

        ProductOptionValue optionValue =
                productOptionValueRepository
                        .findById(request.optionValueId())
                        .orElseThrow(
                                () -> new CommonException(ProductErrorCode.OPTION_VALUE_NOT_FOUND));

        try {
            List<CartRedis> memberCarts = findAllForMember(memberId);

            CartRedis existing =
                    memberCarts.stream()
                            .filter(c -> request.optionValueId().equals(c.getOptionValueId()))
                            .findFirst()
                            .orElse(null);

            if (existing != null) {
                existing.updateQuantity(existing.getQuantity() + request.quantity());
                existing.refreshTtl(TTL_SECONDS);
                CartRedis saved = cartRedisRepository.save(existing);
                log.info(
                        "[CartService] increase quantity: cartId={}, memberId={}, q={}",
                        saved.getCartId(),
                        saved.getMemberId(),
                        saved.getQuantity());
                return saved;
            }

            UUID cartId = UUID.randomUUID();
            CartRedis newCart =
                    CartRedis.of(
                            memberId, cartId, optionValue.getId(), request.quantity(), TTL_SECONDS);

            CartRedis saved = cartRedisRepository.save(newCart);
            log.info(
                    "[CartService] new cart: cartId={}, memberId={}, q={}",
                    saved.getCartId(),
                    saved.getMemberId(),
                    saved.getQuantity());
            return saved;

        } catch (Exception e) {
            log.error(
                    "[CartService] Redis error in createCart (memberId={}, optionValueId={})",
                    memberId,
                    request.optionValueId(),
                    e);
            throw e;
        }
    }

    /** DTO 포함 생성 */
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

    /** 장바구니 수량 수정 */
    public CartRedis updateCart(UUID cartId, CartUpdateRequest request) {
        Long memberId = requireCurrentMemberId();

        if (request.quantity() <= 0) {
            throw new CommonException(CartErrorCode.INVALID_QUANTITY);
        }

        CartRedis cart =
                findAllForMember(memberId).stream()
                        .filter(c -> cartId.equals(c.getCartId()))
                        .findFirst()
                        .orElseThrow(() -> new CommonException(CartErrorCode.CART_EXPIRED));

        cart.updateQuantity(request.quantity());
        cart.refreshTtl(TTL_SECONDS);
        CartRedis saved = cartRedisRepository.save(cart);

        log.info(
                "[CartService] update quantity: cartId={}, memberId={}, quantity={}",
                saved.getCartId(),
                saved.getMemberId(),
                saved.getQuantity());

        return saved;
    }

    /** 장바구니 목록 조회 - 현재 로그인한 회원 기준 */
    public List<CartResponse> getCartListByMember() {
        Long memberId = requireCurrentMemberId();
        log.info("[CartService] getCartListByMember for memberId={}", memberId);

        List<CartRedis> carts = findAllForMember(memberId);

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
                                            .map(d -> d.getAmount())
                                            .orElse(0);

                            return cartMapper.toResponse(cart, optionValue, discount);
                        })
                .collect(Collectors.toList());
    }

    /** 장바구니 삭제 (cartId 기준) */
    public void deleteCart(UUID cartId) {
        Long memberId = requireCurrentMemberId();

        findAllForMember(memberId).stream()
                .filter(c -> cartId.equals(c.getCartId()))
                .findFirst()
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
