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

    private static final long TTL_SECONDS = 600L;

    /** memberId를 nullable 로 가져오기 (안 잡혀도 죽이지 않음) */
    private Long getCurrentMemberIdNullable() {
        MemberDto member = memberUtil.getCurrentMember();
        if (member == null) {
            log.warn("[CartService] getCurrentMemberIdNullable -> member is null");
            return null;
        }
        return member.memberId();
    }

    /** Redis findAll → List 변환 */
    private List<CartRedis> findAllCarts() {
        List<CartRedis> list =
                StreamSupport.stream(cartRedisRepository.findAll().spliterator(), false)
                        .filter(c -> c != null)
                        .collect(Collectors.toList());

        log.info("[CartService] findAllCarts size={}", list.size());
        return list;
    }

    /** 장바구니 아이템 추가 - 같은 optionValueId가 있으면 quantity 증가 */
    public CartRedis createCart(CartCreateRequest request) {
        Long memberId = getCurrentMemberIdNullable();

        if (request.quantity() <= 0) {
            throw new CommonException(CartErrorCode.INVALID_QUANTITY);
        }

        ProductOptionValue optionValue =
                productOptionValueRepository
                        .findById(request.optionValueId())
                        .orElseThrow(
                                () -> new CommonException(ProductErrorCode.OPTION_VALUE_NOT_FOUND));

        try {
            // 1) 전체 카트 중 (필요하다면 나중에 memberId까지 포함해서 필터링)
            List<CartRedis> allCarts = findAllCarts();

            // 같은 옵션 값 있는지 확인
            CartRedis existing =
                    allCarts.stream()
                            .filter(c -> c.getOptionValueId() != null)
                            .filter(c -> optionValue.getId().equals(c.getOptionValueId()))
                            .findFirst()
                            .orElse(null);

            if (existing != null) {
                existing.updateQuantity(existing.getQuantity() + request.quantity());
                CartRedis saved = cartRedisRepository.save(existing);
                log.info(
                        "[CartService] increase quantity: cartId={}, memberId={}, optionValueId={}, quantity={}",
                        saved.getCartId(),
                        saved.getMemberId(),
                        saved.getOptionValueId(),
                        saved.getQuantity());
                return saved;
            }

            // 2) 없으면 새 카트 생성
            UUID cartId = UUID.randomUUID();
            CartRedis newCart =
                    CartRedis.of(
                            memberId,
                            cartId,
                            optionValue.getId(),
                            request.quantity(),
                            TTL_SECONDS);

            CartRedis saved = cartRedisRepository.save(newCart);
            log.info(
                    "[CartService] create new cart: cartId={}, memberId={}, optionValueId={}, quantity={}",
                    saved.getCartId(),
                    saved.getMemberId(),
                    saved.getOptionValueId(),
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

    /** 장바구니 수량 수정 (cartId 기준) */
    public CartRedis updateCart(UUID cartId, CartUpdateRequest request) {
        Long memberId = getCurrentMemberIdNullable();

        CartRedis cart =
                findAllCarts().stream()
                        .filter(c -> c.getCartId() != null)
                        .filter(c -> cartId.equals(c.getCartId()))
                        .findFirst()
                        .orElseThrow(() -> new CommonException(CartErrorCode.CART_EXPIRED));

        if (request.quantity() <= 0) {
            throw new CommonException(CartErrorCode.INVALID_QUANTITY);
        }

        cart.updateQuantity(request.quantity());
        CartRedis saved = cartRedisRepository.save(cart);

        log.info(
                "[CartService] update quantity: cartId={}, memberId={}, quantity={}",
                saved.getCartId(),
                saved.getMemberId(),
                saved.getQuantity());

        return saved;
    }

    /** 장바구니 목록 조회 - memberId가 null이면: 전체 카트 반환 - memberId가 있으면: 그 멤버 것만 필터 */
    public List<CartResponse> getCartListByMember() {
        Long memberId = getCurrentMemberIdNullable();
        log.info("[CartService] getCartListByMember for memberId={}", memberId);

        List<CartRedis> carts = findAllCarts();

        if (memberId != null) {
            carts =
                    carts.stream()
                            .filter(c -> c.getMemberId() != null)
                            .filter(c -> memberId.equals(c.getMemberId()))
                            .collect(Collectors.toList());
        }

        log.info("[CartService] carts for member (after filter) size={}", carts.size());

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

    /** 삭제 (cartId 기준) */
    public void deleteCart(UUID cartId) {
        Long memberId = getCurrentMemberIdNullable();

        findAllCarts().stream()
                .filter(c -> c.getCartId() != null)
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
