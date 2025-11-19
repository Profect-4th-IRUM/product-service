package com.irum.productservice.domain.cart.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.irum.global.advice.exception.CommonException;
import com.irum.openfeign.member.dto.response.MemberDto;
import com.irum.openfeign.member.enums.Role;
import com.irum.productservice.domain.cart.domain.entity.CartItem;
import com.irum.productservice.domain.cart.domain.repository.CartItemRepository;
import com.irum.productservice.domain.cart.dto.request.CartCreateRequest;
import com.irum.productservice.domain.cart.dto.request.CartUpdateRequest;
import com.irum.productservice.domain.cart.dto.response.CartResponse;
import com.irum.productservice.domain.cart.mapper.CartMapper;
import com.irum.productservice.domain.discount.domain.entity.Discount;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @InjectMocks private CartService cartService;

    @Mock private CartItemRepository cartItemRepository;
    @Mock private ProductOptionValueRepository productOptionValueRepository;
    @Mock private DiscountRepository discountRepository;
    @Mock private CartMapper cartMapper;
    @Mock private MemberUtil memberUtil;

    private MemberDto member;
    private UUID optionValueId;
    private UUID productId;
    private String cartItemId;

    @BeforeEach
    void setUp() {
        member = new MemberDto(1L, "테스트유저", "test@test.com", "010-1111-2222", Role.CUSTOMER);

        optionValueId = UUID.randomUUID();
        productId = UUID.randomUUID();
        cartItemId = UUID.randomUUID().toString();
    }

    /** createCart 내부에서 optionValue.getId() 까지만 쓰는 최소 stub */
    private ProductOptionValue stubOptionValueIdOnly() {
        ProductOptionValue optionValue = mock(ProductOptionValue.class);
        when(optionValue.getId()).thenReturn(optionValueId);
        return optionValue;
    }

    @Test
    @DisplayName("createCart — 새 장바구니 생성 성공")
    void createCart_new_success() {
        // given
        CartCreateRequest request = new CartCreateRequest(optionValueId, 2);

        when(memberUtil.getCurrentMember()).thenReturn(member);

        ProductOptionValue optionValue = stubOptionValueIdOnly();
        when(productOptionValueRepository.findById(optionValueId))
                .thenReturn(Optional.of(optionValue));

        when(cartItemRepository.findByMemberIdAndOptionValueId(member.memberId(), optionValueId))
                .thenReturn(Optional.empty());

        when(cartItemRepository.save(any(CartItem.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        CartItem result = cartService.createCart(request);

        // then
        assertThat(result.getMemberId()).isEqualTo(member.memberId());
        assertThat(result.getOptionValueId()).isEqualTo(optionValueId);
        assertThat(result.getQuantity()).isEqualTo(2);
        verify(cartItemRepository).save(any(CartItem.class));
    }

    @Test
    @DisplayName("createCart — 기존 장바구니 존재 → 수량 누적")
    void createCart_existing_accumulateQuantity() {
        // given
        CartCreateRequest request = new CartCreateRequest(optionValueId, 3);

        when(memberUtil.getCurrentMember()).thenReturn(member);

        ProductOptionValue optionValue = stubOptionValueIdOnly();
        when(productOptionValueRepository.findById(optionValueId))
                .thenReturn(Optional.of(optionValue));

        CartItem existing = CartItem.of(member.memberId(), optionValueId, 2, 60L);
        when(cartItemRepository.findByMemberIdAndOptionValueId(member.memberId(), optionValueId))
                .thenReturn(Optional.of(existing));

        when(cartItemRepository.save(any(CartItem.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        CartItem result = cartService.createCart(request);

        // then
        assertThat(result.getQuantity()).isEqualTo(5);
        verify(cartItemRepository).save(existing);
    }

    @Test
    @DisplayName("createCart — quantity <= 0 이면 INVALID_QUANTITY")
    void createCart_invalidQuantity() {
        // given
        CartCreateRequest request = new CartCreateRequest(optionValueId, 0);
        when(memberUtil.getCurrentMember()).thenReturn(member);

        // when
        CommonException ex =
                assertThrows(CommonException.class, () -> cartService.createCart(request));

        // then
        assertThat(ex.getErrorCode()).isEqualTo(CartErrorCode.INVALID_QUANTITY);
        verify(productOptionValueRepository, never()).findById(any());
        verify(cartItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("createCart — 옵션 값이 없으면 OPTION_VALUE_NOT_FOUND")
    void createCart_optionNotFound() {
        // given
        CartCreateRequest request = new CartCreateRequest(optionValueId, 1);
        when(memberUtil.getCurrentMember()).thenReturn(member);
        when(productOptionValueRepository.findById(optionValueId)).thenReturn(Optional.empty());

        // when
        CommonException ex =
                assertThrows(CommonException.class, () -> cartService.createCart(request));

        // then
        assertThat(ex.getErrorCode()).isEqualTo(ProductErrorCode.OPTION_VALUE_NOT_FOUND);
        verify(cartItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("createCartWithResponse — 할인 적용 + 응답 DTO 매핑")
    void createCartWithResponse_discountAndMapping() {
        // given
        CartCreateRequest request = new CartCreateRequest(optionValueId, 2);
        when(memberUtil.getCurrentMember()).thenReturn(member);

        // 1) createCart() 에서 사용할 최소 optionValue (getId만)
        ProductOptionValue optionValueForCreate = stubOptionValueIdOnly();

        // 2) createCartWithResponse() 내부에서 할인 계산/매핑용 optionValue
        Product product = mock(Product.class);
        when(product.getId()).thenReturn(productId);

        ProductOptionGroup group = mock(ProductOptionGroup.class);
        when(group.getProduct()).thenReturn(product);

        ProductOptionValue optionValueForMapping = mock(ProductOptionValue.class);
        when(optionValueForMapping.getOptionGroup()).thenReturn(group);

        when(productOptionValueRepository.findById(optionValueId))
                .thenReturn(Optional.of(optionValueForCreate))
                .thenReturn(Optional.of(optionValueForMapping));

        CartItem cartItem = CartItem.of(member.memberId(), optionValueId, 2, 60L);
        when(cartItemRepository.findByMemberIdAndOptionValueId(member.memberId(), optionValueId))
                .thenReturn(Optional.empty());
        when(cartItemRepository.save(any(CartItem.class))).thenReturn(cartItem);

        Discount discount = mock(Discount.class);
        when(discount.getAmount()).thenReturn(1_000);
        when(discountRepository.findByProductId(productId)).thenReturn(Optional.of(discount));

        CartResponse mapped =
                CartResponse.builder()
                        .cartItemId(cartItem.getCartItemId())
                        .optionValueId(optionValueId)
                        .productName("테스트 상품")
                        .optionValueName("옵션A")
                        .imageUrl("image")
                        .quantity(2)
                        .basePrice(10_000)
                        .extraPrice(500)
                        .discountAmount(1_000)
                        .unitPrice(9_500)
                        .lineTotal(19_000)
                        .stockQuantity(5)
                        .build();

        when(cartMapper.toResponse(cartItem, optionValueForMapping, 1_000)).thenReturn(mapped);

        // when
        CartResponse result = cartService.createCartWithResponse(request);

        // then
        assertThat(result.cartItemId()).isEqualTo(cartItem.getCartItemId());
        assertThat(result.discountAmount()).isEqualTo(1_000);
        verify(discountRepository).findByProductId(productId);
        verify(cartMapper).toResponse(cartItem, optionValueForMapping, 1_000);
    }

    @Test
    @DisplayName("updateCart — 수량 수정 성공")
    void updateCart_success() {
        // given
        when(memberUtil.getCurrentMember()).thenReturn(member);

        CartUpdateRequest request = new CartUpdateRequest(5);
        CartItem existing = CartItem.of(member.memberId(), optionValueId, 2, 60L);

        when(cartItemRepository.findByMemberIdAndCartItemId(member.memberId(), cartItemId))
                .thenReturn(Optional.of(existing));
        when(cartItemRepository.save(any(CartItem.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        CartItem updated = cartService.updateCart(cartItemId, request);

        // then
        assertThat(updated.getQuantity()).isEqualTo(5);
        verify(cartItemRepository).save(existing);
    }

    @Test
    @DisplayName("updateCart — quantity <= 0 → INVALID_QUANTITY")
    void updateCart_invalidQuantity() {
        // given
        when(memberUtil.getCurrentMember()).thenReturn(member);
        CartUpdateRequest request = new CartUpdateRequest(0);

        // when
        CommonException ex =
                assertThrows(
                        CommonException.class, () -> cartService.updateCart(cartItemId, request));

        // then
        assertThat(ex.getErrorCode()).isEqualTo(CartErrorCode.INVALID_QUANTITY);
        verify(cartItemRepository, never()).findByMemberIdAndCartItemId(anyLong(), anyString());
    }

    @Test
    @DisplayName("updateCart — Redis 에 장바구니 없으면 CART_EXPIRED")
    void updateCart_expired() {
        // given
        when(memberUtil.getCurrentMember()).thenReturn(member);
        CartUpdateRequest request = new CartUpdateRequest(3);
        when(cartItemRepository.findByMemberIdAndCartItemId(member.memberId(), cartItemId))
                .thenReturn(Optional.empty());

        // when
        CommonException ex =
                assertThrows(
                        CommonException.class, () -> cartService.updateCart(cartItemId, request));

        // then
        assertThat(ex.getErrorCode()).isEqualTo(CartErrorCode.CART_EXPIRED);
        verify(cartItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("getCartListByMember — 정상 조회")
    void getCartListByMember_success() {
        // given
        when(memberUtil.getCurrentMember()).thenReturn(member);

        CartItem cartItem = CartItem.of(member.memberId(), optionValueId, 2, 60L);
        when(cartItemRepository.findByMemberId(member.memberId())).thenReturn(List.of(cartItem));

        Product product = mock(Product.class);
        when(product.getId()).thenReturn(productId);

        ProductOptionGroup group = mock(ProductOptionGroup.class);
        when(group.getProduct()).thenReturn(product);

        ProductOptionValue optionValue = mock(ProductOptionValue.class);
        when(optionValue.getOptionGroup()).thenReturn(group);

        when(productOptionValueRepository.findById(optionValueId))
                .thenReturn(Optional.of(optionValue));

        Discount discount = mock(Discount.class);
        when(discount.getAmount()).thenReturn(1_000);
        when(discountRepository.findByProductId(productId)).thenReturn(Optional.of(discount));

        CartResponse mapped =
                CartResponse.builder()
                        .cartItemId(cartItem.getCartItemId())
                        .optionValueId(optionValueId)
                        .productName("테스트 상품")
                        .optionValueName("옵션A")
                        .imageUrl("image")
                        .quantity(2)
                        .basePrice(10_000)
                        .extraPrice(500)
                        .discountAmount(1_000)
                        .unitPrice(9_500)
                        .lineTotal(19_000)
                        .stockQuantity(5)
                        .build();

        when(cartMapper.toResponse(cartItem, optionValue, 1_000)).thenReturn(mapped);

        // when
        List<CartResponse> result = cartService.getCartListByMember();

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).cartItemId()).isEqualTo(cartItem.getCartItemId());
        verify(cartItemRepository).findByMemberId(member.memberId());
    }

    @Test
    @DisplayName("deleteCart — 장바구니 존재 시 삭제 수행")
    void deleteCart_success() {
        // given
        when(memberUtil.getCurrentMember()).thenReturn(member);

        CartItem cartItem = CartItem.of(member.memberId(), optionValueId, 2, 60L);
        when(cartItemRepository.findByMemberIdAndCartItemId(member.memberId(), cartItemId))
                .thenReturn(Optional.of(cartItem));

        // when
        cartService.deleteCart(cartItemId);

        // then
        verify(cartItemRepository).deleteById(cartItem.getCartItemId());
    }
}
