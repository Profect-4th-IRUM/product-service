package com.irum.productservice.domain.cart.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.irum.global.advice.exception.CommonException;
import com.irum.productservice.domain.cart.domain.entity.CartRedis;
import com.irum.productservice.domain.cart.domain.repository.CartRedisRepository;
import com.irum.productservice.domain.cart.dto.request.CartCreateRequest;
import com.irum.productservice.domain.cart.dto.request.CartUpdateRequest;
import com.irum.productservice.domain.cart.dto.response.CartResponse;
import com.irum.productservice.domain.cart.mapper.CartMapper;
import com.irum.productservice.domain.discount.domain.repository.DiscountRepository;
import com.irum.productservice.domain.product.domain.entity.Product;
import com.irum.productservice.domain.product.domain.entity.ProductOptionGroup;
import com.irum.productservice.domain.product.domain.entity.ProductOptionValue;
import com.irum.productservice.domain.product.domain.repository.ProductOptionValueRepository;
import com.irum.productservice.global.exception.errorcode.CartErrorCode;
import com.irum.productservice.global.exception.errorcode.ProductErrorCode;
import com.irum.productservice.global.util.MemberUtil;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock private CartRedisRepository cartRedisRepository;
    @Mock private ProductOptionValueRepository productOptionValueRepository;
    @Mock private DiscountRepository discountRepository;
    @Mock private CartMapper cartMapper;

    // getCurrentMember().memberId() 까지 체이닝을 써서 mock 해야 하므로 deep stub 사용
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private MemberUtil memberUtil;

    @InjectMocks private CartService cartService;

    private final UUID optionValueId = UUID.randomUUID();
    private final UUID productId = UUID.randomUUID();
    private final UUID cartId = UUID.randomUUID();
    private final Long memberId = 1L;

    private ProductOptionValue stubOptionValue() {
        Product product = mock(Product.class);
        when(product.getId()).thenReturn(productId);
        when(product.getPrice()).thenReturn(10_000);

        ProductOptionGroup group = mock(ProductOptionGroup.class);
        when(group.getProduct()).thenReturn(product);

        ProductOptionValue optionValue = mock(ProductOptionValue.class);
        when(optionValue.getId()).thenReturn(optionValueId);
        when(optionValue.getOptionGroup()).thenReturn(group);
        when(optionValue.getExtraPrice()).thenReturn(500);
        when(optionValue.getStockQuantity()).thenReturn(5);

        return optionValue;
    }

    @Test
    @DisplayName("createCartWithResponse: 신규 장바구니 생성 성공")
    void createCartWithResponse_success() {
        // given
        CartCreateRequest request = new CartCreateRequest(optionValueId, 2);

        when(memberUtil.getCurrentMember().memberId()).thenReturn(memberId);

        ProductOptionValue optionValue = stubOptionValue();
        when(productOptionValueRepository.findById(optionValueId))
                .thenReturn(Optional.of(optionValue));
        when(cartRedisRepository.findByMemberIdAndOptionValueId(memberId, optionValueId))
                .thenReturn(Optional.empty());
        when(cartRedisRepository.save(any(CartRedis.class))).thenAnswer(inv -> inv.getArgument(0));

        when(discountRepository.findByProductId(productId)).thenReturn(Optional.empty());

        CartResponse mapped =
                CartResponse.builder()
                        .cartId(cartId)
                        .optionValueId(optionValueId)
                        .productName("테스트 상품")
                        .optionValueName("옵션A")
                        .imageUrl("url")
                        .quantity(2)
                        .basePrice(10_000)
                        .extraPrice(500)
                        .discountAmount(0)
                        .unitPrice(10_500)
                        .lineTotal(21_000)
                        .stockQuantity(5)
                        .build();

        when(cartMapper.toResponse(any(CartRedis.class), eq(optionValue), eq(0)))
                .thenReturn(mapped);

        // when
        CartResponse result = cartService.createCartWithResponse(request);

        // then
        assertThat(result).isNotNull();
        assertThat(result.quantity()).isEqualTo(2);
        verify(cartRedisRepository).save(any(CartRedis.class));
    }

    @Test
    @DisplayName("createCartWithResponse: quantity <= 0 → INVALID_QUANTITY")
    void createCartWithResponse_invalidQuantity() {
        // given
        CartCreateRequest request = new CartCreateRequest(optionValueId, 0);
        when(memberUtil.getCurrentMember().memberId()).thenReturn(memberId);

        // when
        CommonException ex =
                assertThrows(
                        CommonException.class, () -> cartService.createCartWithResponse(request));

        // then
        assertThat(ex.getErrorCode()).isEqualTo(CartErrorCode.INVALID_QUANTITY);
        verify(productOptionValueRepository, never()).findById(any());
        verify(cartRedisRepository, never()).save(any());
    }

    @Test
    @DisplayName("createCartWithResponse: 옵션 없으면 OPTION_VALUE_NOT_FOUND")
    void createCartWithResponse_optionNotFound() {
        // given
        CartCreateRequest request = new CartCreateRequest(optionValueId, 1);
        when(memberUtil.getCurrentMember().memberId()).thenReturn(memberId);
        when(productOptionValueRepository.findById(optionValueId)).thenReturn(Optional.empty());

        // when
        CommonException ex =
                assertThrows(
                        CommonException.class, () -> cartService.createCartWithResponse(request));

        // then
        assertThat(ex.getErrorCode()).isEqualTo(ProductErrorCode.OPTION_VALUE_NOT_FOUND);
        verify(cartRedisRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateCart: 수량 수정 성공")
    void updateCart_success() {
        // given
        CartUpdateRequest request = new CartUpdateRequest(5);
        when(memberUtil.getCurrentMember().memberId()).thenReturn(memberId);

        CartRedis existing = CartRedis.of(memberId, cartId, optionValueId, 2, 30L);

        when(cartRedisRepository.findByMemberIdAndCartId(memberId, cartId))
                .thenReturn(Optional.of(existing));
        when(cartRedisRepository.save(any(CartRedis.class))).thenAnswer(inv -> inv.getArgument(0));

        // when
        CartRedis updated = cartService.updateCart(cartId, request);

        // then
        assertThat(updated.getQuantity()).isEqualTo(5);
        verify(cartRedisRepository).save(existing);
    }

    @Test
    @DisplayName("updateCart: Redis에 없으면 CART_EXPIRED")
    void updateCart_expired() {
        // given
        CartUpdateRequest request = new CartUpdateRequest(3);
        when(memberUtil.getCurrentMember().memberId()).thenReturn(memberId);
        when(cartRedisRepository.findByMemberIdAndCartId(memberId, cartId))
                .thenReturn(Optional.empty());

        // when
        CommonException ex =
                assertThrows(CommonException.class, () -> cartService.updateCart(cartId, request));

        // then
        assertThat(ex.getErrorCode()).isEqualTo(CartErrorCode.CART_EXPIRED);
        verify(cartRedisRepository, never()).save(any());
    }

    @Test
    @DisplayName("getCartListByMember: 장바구니 목록 조회 성공")
    void getCartListByMember_success() {
        // given
        when(memberUtil.getCurrentMember().memberId()).thenReturn(memberId);

        CartRedis cart = CartRedis.of(memberId, cartId, optionValueId, 2, 30L);
        when(cartRedisRepository.findByMemberId(memberId)).thenReturn(List.of(cart));

        ProductOptionValue optionValue = stubOptionValue();
        when(productOptionValueRepository.findById(optionValueId))
                .thenReturn(Optional.of(optionValue));

        when(discountRepository.findByProductId(productId)).thenReturn(Optional.empty());

        CartResponse mapped =
                CartResponse.builder()
                        .cartId(cartId)
                        .optionValueId(optionValueId)
                        .productName("테스트 상품")
                        .optionValueName("옵션A")
                        .imageUrl("url")
                        .quantity(2)
                        .basePrice(10_000)
                        .extraPrice(500)
                        .discountAmount(0)
                        .unitPrice(10_500)
                        .lineTotal(21_000)
                        .stockQuantity(5)
                        .build();

        when(cartMapper.toResponse(cart, optionValue, 0)).thenReturn(mapped);

        // when
        var result = cartService.getCartListByMember();

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).cartId()).isEqualTo(cartId);
        verify(cartRedisRepository).findByMemberId(memberId);
    }

    @Test
    @DisplayName("deleteCart: 회원 기준 장바구니 삭제 호출")
    void deleteCart_success() {
        // given
        when(memberUtil.getCurrentMember().memberId()).thenReturn(memberId);

        // when
        cartService.deleteCart(cartId);

        // then
        verify(cartRedisRepository).deleteByMemberIdAndCartId(memberId, cartId);
    }
}
