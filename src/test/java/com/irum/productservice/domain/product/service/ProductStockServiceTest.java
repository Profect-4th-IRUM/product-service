package com.irum.productservice.domain.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.lenient;

import com.irum.openfeign.product.dto.request.ProductInternalRequest;
import com.irum.openfeign.product.dto.request.UpdateStockRequest;
import com.irum.openfeign.product.dto.response.ProductInternalResponse;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ProductStockServiceTest {

    private ProductStockService productStockService;

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

        productStockService =
                new ProductStockService(
                        productOptionValueRepository,
                        discountRepository,
                        storeRepository,
                        updateStockMapper);
    }

    @Test
    @DisplayName("재고 업데이트 성공 - 옵션 재고가 요청 수량만큼 감소")
    void updateStockInTransaction_success() {
        // given
        // 1) 요청 DTO
        ProductInternalRequest.OptionValueRequest reqOption1 =
                new ProductInternalRequest.OptionValueRequest(optionValueId1, 5);
        ProductInternalRequest.OptionValueRequest reqOption2 =
                new ProductInternalRequest.OptionValueRequest(optionValueId2, 10);
        ProductInternalRequest request =
                new ProductInternalRequest(List.of(reqOption1, reqOption2), storeId);

        // Store
        Store mockStore =
                Store.createStore(
                        "테스트 상점", "010-1234-5678", "서울시 강남구", "1234567890", "2025123456", 1L);
        ReflectionTestUtils.setField(mockStore, "id", storeId);

        // Category
        Category testCategory = org.mockito.Mockito.mock(Category.class);
        lenient().when(testCategory.getDepth()).thenReturn(3); // 🔥 꼭 필요

        // Delivery Policy
        DeliveryPolicy mockPolicy = org.mockito.Mockito.mock(DeliveryPolicy.class);
        lenient().when(mockPolicy.getDefaultDeliveryFee()).thenReturn(3000);
        lenient().when(mockPolicy.getMinAmount()).thenReturn(0);
        lenient().when(mockPolicy.getMinQuantity()).thenReturn(0);
        ReflectionTestUtils.setField(mockStore, "deliveryPolicy", mockPolicy);

        // Product 구조 생성
        Product mockProduct =
                Product.createProduct(
                        mockStore, testCategory, "테스트 상품", "상품 설명", "상품 상세", 10000, true);

        ProductOptionGroup mockGroup = ProductOptionGroup.createOptionGroup(mockProduct, "색상");

        ProductOptionValue pov1 = ProductOptionValue.createOptionValue(mockGroup, "빨강", 100, 0);

        ProductOptionValue pov2 = ProductOptionValue.createOptionValue(mockGroup, "파랑", 50, 0);

        ReflectionTestUtils.setField(pov1, "id", optionValueId1);
        ReflectionTestUtils.setField(pov2, "id", optionValueId2);

        // Repo stubbing
        lenient()
                .when(storeRepository.findByIdWithDeliveryPolicy(storeId))
                .thenReturn(Optional.of(mockStore));

        lenient()
                .when(productOptionValueRepository.findAllByIdWithFetchJoin(anyList()))
                .thenReturn(List.of(pov1, pov2));

        lenient().when(discountRepository.findAllByProductIds(anyList())).thenReturn(List.of());

        // when
        ProductInternalResponse result = productStockService.updateStockInTransaction(request);

        assertThat(result).isNotNull();
        assertThat(pov1.getStockQuantity()).isEqualTo(95);
        assertThat(pov2.getStockQuantity()).isEqualTo(40);
    }
}
