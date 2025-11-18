package com.irum.productservice.domain.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;

import com.irum.openfeign.product.dto.request.UpdateStockRequest;
import com.irum.openfeign.product.dto.response.UpdateStockDto;
import com.irum.productservice.domain.category.domain.entity.Category;
import com.irum.productservice.domain.deliverypolicy.domain.entity.DeliveryPolicy;
import com.irum.productservice.domain.discount.domain.repository.DiscountRepository;
import com.irum.productservice.domain.product.Internal.service.ProductStockService;
import com.irum.productservice.domain.product.domain.entity.Product;
import com.irum.productservice.domain.product.domain.entity.ProductOptionGroup;
import com.irum.productservice.domain.product.domain.entity.ProductOptionValue;
import com.irum.productservice.domain.product.domain.repository.ProductOptionValueRepository;
import com.irum.productservice.domain.product.mapper.UpdateStockMapper;
import com.irum.productservice.domain.store.domain.entity.Store;
import com.irum.productservice.domain.store.domain.repository.StoreRepository;
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
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ProductStockServiceTest {

    @InjectMocks private ProductStockService productStockService;

    @Mock private StoreRepository storeRepository;

    @Mock private ProductOptionValueRepository productOptionValueRepository;

    @Mock private DiscountRepository discountRepository;

    @Mock private UpdateStockMapper updateStockMapper;

    private UUID storeId;
    private UUID optionValueId1;
    private UUID optionValueId2;

    @BeforeEach
    void setUp() {
        storeId = UUID.randomUUID();
        optionValueId1 = UUID.randomUUID();
        optionValueId2 = UUID.randomUUID();
    }

    @Test
    @DisplayName("재고 업데이트 성공 - 옵션 재고가 요청 수량만큼 감소")
    void updateStockInTransaction_success() {
        // given
        // 1) 요청 DTO
        UpdateStockRequest.OptionValueRequest reqOption1 =
                new UpdateStockRequest.OptionValueRequest(optionValueId1, 5);
        UpdateStockRequest.OptionValueRequest reqOption2 =
                new UpdateStockRequest.OptionValueRequest(optionValueId2, 10);
        UpdateStockRequest request =
                new UpdateStockRequest(List.of(reqOption1, reqOption2), storeId);

        // 2) 상점
        Store mockStore =
                Store.createStore(
                        "테스트 상점", "010-1234-5678", "서울시 강남구", "1234567890", "2025123456", 1L);
        ReflectionTestUtils.setField(mockStore, "id", storeId);

        // 3) 카테고리 (깊이 3짜리 카테고리)
        Category testCategory = org.mockito.Mockito.mock(Category.class);
        given(testCategory.getDepth()).willReturn(3);

        // 4) 배송 정책
        DeliveryPolicy mockPolicy = org.mockito.Mockito.mock(DeliveryPolicy.class);
        given(mockPolicy.getDefaultDeliveryFee()).willReturn(3000);
        ReflectionTestUtils.setField(mockStore, "deliveryPolicy", mockPolicy);

        // 5) 상품 / 옵션 그룹 / 옵션 값들
        Product mockProduct =
                Product.createProduct(
                        mockStore, testCategory, "테스트 상품", "상품 설명", "상품 상세 설명", 10_000, true);

        ProductOptionGroup mockOptionGroup =
                ProductOptionGroup.createOptionGroup(mockProduct, "색상");

        ProductOptionValue pov1 =
                ProductOptionValue.createOptionValue(mockOptionGroup, "빨강", 100, 0);
        ProductOptionValue pov2 =
                ProductOptionValue.createOptionValue(mockOptionGroup, "파랑", 50, 0);

        ReflectionTestUtils.setField(pov1, "id", optionValueId1);
        ReflectionTestUtils.setField(pov2, "id", optionValueId2);

        // 6) Repository stubbing
        given(storeRepository.findByIdWithDeliveryPolicy(storeId))
                .willReturn(Optional.of(mockStore));

        given(productOptionValueRepository.findAllByIdWithFetchJoin(anyList()))
                .willReturn(List.of(pov1, pov2));

        // 할인 로직이 내부에서 호출될 수도 있으므로 lenient 로 처리
        lenient().when(discountRepository.findAllByProductIds(anyList())).thenReturn(List.of());

        // when
        UpdateStockDto result = productStockService.updateStockInTransaction(request);

        // then
        // 1) 반환 DTO는 null 이 아니어야 함 (세부 필드는 서비스 구현에 따라 다를 수 있으므로 여기서는 존재 여부만 검증)
        assertThat(result).isNotNull();

        // 2) 재고가 정확히 감소했는지 검증
        assertThat(pov1.getStockQuantity()).isEqualTo(95); // 100 - 5
        assertThat(pov2.getStockQuantity()).isEqualTo(40); // 50 - 10
    }
}
