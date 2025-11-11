package com.irum.productservice.domain.product.service;

import com.irum.openfeign.member.dto.response.MemberDto;
import com.irum.productservice.domain.category.domain.entity.Category;
import com.irum.productservice.domain.category.domain.repository.CategoryRepository;
import com.irum.productservice.domain.product.domain.entity.Product;
import com.irum.productservice.domain.product.domain.repository.ProductOptionGroupRepository;
import com.irum.productservice.domain.product.domain.repository.ProductOptionValueRepository;
import com.irum.productservice.domain.product.domain.repository.ProductRepository;
import com.irum.productservice.domain.product.dto.request.ProductCreateRequest;
import com.irum.productservice.domain.store.domain.entity.Store;
import com.irum.productservice.domain.store.domain.repository.StoreRepository;
import com.irum.productservice.global.util.MemberUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @InjectMocks
    private ProductService productService;

    @Mock
    private ProductRepository productRepository;
    @Mock
    private ProductOptionGroupRepository optionGroupRepository;
    @Mock
    private ProductOptionValueRepository optionValueRepository;
    @Mock
    private StoreRepository storeRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private MemberUtil memberUtil;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private MemberDto member;
    private Store store;
    private Category category;

    @BeforeEach
    void setUp() throws Exception {
        member = new MemberDto(1L, "이삭", "isak@test.com", "010-1111-1111", com.irum.openfeign.member.enums.Role.OWNER);

        // Store Mock
        store = Store.createStore(
                "상점1",
                "010-2222-3333",
                "서울 강남구",
                "1234567890",
                "0987654321",
                member.memberId()
        );

        // ✅ Root + Sub Category 직접 생성 (mock save 제거)
        Category root = Category.createRootCategory("루트");
        Category parent = Category.createSubCategory("부모", root);
        category = Category.createSubCategory("자식", parent);
    }
    @DisplayName("✅ 상품 생성 성공 테스트 - 옵션 없이")
    @Test
    void createProduct_Success_NoOption() {
        // given
        UUID categoryId = UUID.randomUUID();
        when(memberUtil.getCurrentMember()).thenReturn(member);
        when(storeRepository.findByMember(member.memberId())).thenReturn(Optional.of(store));
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));

        ProductCreateRequest request = new ProductCreateRequest(
                "상품1",
                "간단한 설명",
                "상세 설명",
                true,
                1000,
                categoryId,
                null // 옵션 그룹 없음
        );

        // when
        productService.createProduct(request);

        // then
        verify(productRepository, times(1)).save(any(Product.class));
        verify(storeRepository, times(1)).findByMember(member.memberId());
        verify(categoryRepository, times(1)).findById(categoryId);
        System.out.println("🟢 상품 생성 성공 테스트 (옵션 없음) 통과");
    }

}