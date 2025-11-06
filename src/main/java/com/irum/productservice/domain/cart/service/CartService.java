package com.irum.productservice.domain.cart.service;

import com.irum.productservice.domain.cart.domain.entity.Cart;
import com.irum.productservice.domain.cart.domain.repository.CartRepository;
import com.irum.productservice.domain.cart.dto.request.CartCreateRequest;
import com.irum.productservice.domain.cart.dto.request.CartUpdateRequest;
import com.irum.productservice.domain.cart.dto.response.CartResponse;
import com.irum.productservice.domain.member.domain.entity.Member;
import com.irum.productservice.domain.product.domain.entity.ProductOptionValue;
import com.irum.productservice.domain.product.domain.repository.ProductOptionValueRepository;
import com.irum.productservice.global.exception.errorcode.CartErrorCode;
import com.irum.productservice.global.util.MemberUtil;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CartService {

    private final CartRepository cartRepository;
    private final ProductOptionValueRepository productOptionValueRepository;
    private final MemberUtil memberUtil;

    /** 장바구니 생성 */
    public CartResponse createCart(CartCreateRequest request) {
        Member currentMember = memberUtil.getCurrentMember();

        ProductOptionValue optionValue = productOptionValueRepository
                .findById(request.optionValueId())
                .orElseThrow(() -> new CommonException(CartErrorCode.OPTION_VALUE_NOT_FOUND));

        Cart existing = cartRepository.findByMemberIdAndOptionValueId(
                currentMember.getMemberId(), request.optionValueId());

        Cart target;
        if (existing != null) {
            existing.updateQuantity(existing.getQuantity() + request.quantity());
            target = existing;
        } else {
            target = Cart.createCart(currentMember, optionValue, request.quantity());
            cartRepository.save(target);
        }

        return CartResponse.from(target);
    }

    /** 장바구니 수정 */
    public void updateCart(UUID cartId, CartUpdateRequest request) {
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new CommonException(CartErrorCode.CART_NOT_FOUND));

        memberUtil.assertMemberResourceAccess(cart.getMember());
        cart.updateQuantity(request.quantity());
    }

    /** 내 장바구니 목록 조회 */
    @Transactional(readOnly = true)
    public List<CartResponse> getCartListByMember() {
        Member currentMember = memberUtil.getCurrentMember();
        List<Cart> carts = cartRepository.findAllWithProductByMemberId(currentMember.getMemberId());
        return carts.stream()
                .map(CartResponse::from)
                .collect(Collectors.toList());
    }

    /** 장바구니 삭제 */
    public void deleteCart(UUID cartId) {
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new CommonException(CartErrorCode.CART_NOT_FOUND));

        memberUtil.assertMemberResourceAccess(cart.getMember());
        cart.softDelete(memberUtil.getCurrentMember().getMemberId());
    }
}
