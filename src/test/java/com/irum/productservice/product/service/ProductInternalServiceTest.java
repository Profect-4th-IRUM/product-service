package com.irum.productservice.product.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

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

import com.irum.openfeign.dto.request.UpdateStockRequest;
import com.irum.openfeign.dto.response.UpdateStockDto;
import com.irum.productservice.domain.category.domain.entity.Category;
import com.irum.productservice.domain.discount.domain.repository.DiscountRepository;
import com.irum.productservice.domain.product.Internal.service.ProductInternalService;
import com.irum.productservice.domain.product.domain.entity.Product;
import com.irum.productservice.domain.product.domain.entity.ProductOptionGroup;
import com.irum.productservice.domain.product.domain.entity.ProductOptionValue;
import com.irum.productservice.domain.product.domain.repository.ProductOptionValueRepository;
import com.irum.productservice.domain.product.domain.repository.ProductRepository;
import com.irum.productservice.domain.store.domain.entity.Store;
import com.irum.productservice.domain.store.domain.repository.StoreRepository;

@ExtendWith(MockitoExtension.class)
public class ProductInternalServiceTest {
	@InjectMocks
	private ProductInternalService productInternalService;

	@Mock
	private StoreRepository storeRepository;
	@Mock
	private ProductOptionValueRepository productOptionValueRepository;
	@Mock
	private DiscountRepository discountRepository;


	// 테스트에 필요한 공통 변수 (UUID 등)
	private UUID storeId;
	private UUID optionValueId1;
	private UUID optionValueId2;

	@BeforeEach
	void setUp() {
		// 테스트 전 공통 ID 설정
		storeId = UUID.randomUUID();
		optionValueId1 = UUID.randomUUID();
		optionValueId2 = UUID.randomUUID();
	}

	@Test
	@DisplayName("재고 업데이트 성공")
	void updateStock_Success(){
		// given (준비)
		// 1. 요청(Request) 객체 생성
		UpdateStockRequest.OptionValueRequest reqOption1 = new UpdateStockRequest.OptionValueRequest(optionValueId1, 5); // 5개 요청
		UpdateStockRequest.OptionValueRequest reqOption2 = new UpdateStockRequest.OptionValueRequest(optionValueId2, 10); // 10개 요청
		UpdateStockRequest request = new UpdateStockRequest(List.of(reqOption1, reqOption2), storeId);

		// 2. Mock 객체들이 반환할 데이터 생성
		Store mockStore = Store.createStore(
			"테스트 상점",
			"010-1234-5678",
			"서울시 강남구",
			"1234567890",
			"2025123456",
			1L
		);
		// 1. Category 클래스의 Mock(가짜) 객체를 생성합니다.
		Category testCategory = mock(Category.class);
		given(testCategory.getDepth()).willReturn(3);

		Product mockProduct = Product.createProduct(
			mockStore,
			testCategory,
			"테스트 상품",
			"상품 설명",
			"상품 상세 설명",
			10000,
			true
		);
		ProductOptionGroup mockOptionGroup = ProductOptionGroup.createOptionGroup(mockProduct, "옵션");
		ProductOptionValue pov1 = ProductOptionValue.createOptionValue(
			mockOptionGroup,
			"빨강",
			100,
			0
		); //재고 100개
		ProductOptionValue pov2 = ProductOptionValue.createOptionValue(
			mockOptionGroup,
			"파랑",
			50,
			0
		); // 재고 50개

		// 3. Repository Mock 객체의 행동 정의 (Stubbing)
		given(storeRepository.findByIdWithDeliveryPolicy(storeId))
			.willReturn(Optional.of(mockStore));

		given(productOptionValueRepository.findAllByIdWithFetchJoin(anyList()))
			.willReturn(List.of(pov1, pov2));

		given(discountRepository.findAllByProductIds(anyList()))
			.willReturn(List.of()); // 할인은 이 테스트의 핵심이 아니므로 빈 리스트 반환

		// when (실행)
		UpdateStockDto resultDto = productInternalService.updateStock(request);

		// then (검증)
		// 1. 반환된 DTO 검증
		assertThat(resultDto).isNotNull();

		// 2. 핵심 검증: 재고가 요청한 수량만큼 정확히 감소했는지 확인
		assertThat(pov1.getStockQuantity()).isEqualTo(95); // 100 - 5
		assertThat(pov2.getStockQuantity()).isEqualTo(40); // 50 - 10
	}
}
