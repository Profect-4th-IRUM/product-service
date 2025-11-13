package com.irum.productservice.domain.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.irum.global.advice.exception.CommonException;
import com.irum.openfeign.member.dto.response.MemberDto;
import com.irum.productservice.domain.category.domain.entity.Category;
import com.irum.productservice.domain.category.domain.repository.CategoryRepository;
import com.irum.productservice.domain.product.domain.entity.Product;
import com.irum.productservice.domain.product.domain.entity.ProductOptionGroup;
import com.irum.productservice.domain.product.domain.repository.ProductOptionGroupRepository;
import com.irum.productservice.domain.product.domain.repository.ProductOptionValueRepository;
import com.irum.productservice.domain.product.domain.repository.ProductRepository;
import com.irum.productservice.domain.product.dto.request.*;
import com.irum.productservice.domain.product.event.ProductDeletedEvent;
import com.irum.productservice.domain.store.domain.entity.Store;
import com.irum.productservice.domain.store.domain.repository.StoreRepository;
import com.irum.productservice.global.util.MemberUtil;
import java.lang.reflect.Field;
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
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @InjectMocks private ProductService productService;

    @Mock private ProductRepository productRepository;
    @Mock private ProductOptionGroupRepository optionGroupRepository;
    @Mock private ProductOptionValueRepository optionValueRepository;
    @Mock private StoreRepository storeRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private MemberUtil memberUtil;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private ProductOptionGroupRepository productOptionGroupRepository;
    @Mock private ProductOptionValueRepository productOptionValueRepository;

    private MemberDto member;
    private Store store;
    private Category category;
    private UUID storeId;
    private UUID productId;
    private Product product;
    private UUID optionGroupId;
    private ProductOptionGroup optionGroup;

    @BeforeEach
    void setUp() throws Exception {
        member =
                new MemberDto(
                        1L,
                        "이삭",
                        "isak@test.com",
                        "010-1111-1111",
                        com.irum.openfeign.member.enums.Role.OWNER);

        storeId = UUID.randomUUID();
        // Store Mock
        store =
                Store.createStore(
                        "상점1",
                        "010-2222-3333",
                        "서울 강남구",
                        "1234567890",
                        "0987654321",
                        member.memberId());

        Field storeIdField = Store.class.getDeclaredField("id");
        storeIdField.setAccessible(true);
        storeIdField.set(store, storeId);

        // Root + Sub Category 직접 생성
        Category rootCategory = Category.createRootCategory("루트 카테고리");
        Field rootIdField = Category.class.getDeclaredField("categoryId");
        rootIdField.setAccessible(true);
        rootIdField.set(rootCategory, UUID.randomUUID());

        Category parentCategory = Category.createSubCategory("부모 카테고리", rootCategory);
        Field parentIdField = Category.class.getDeclaredField("categoryId");
        parentIdField.setAccessible(true);
        parentIdField.set(parentCategory, UUID.randomUUID());

        category = Category.createSubCategory("자식 카테고리", parentCategory);
        Field subIdField = Category.class.getDeclaredField("categoryId");
        subIdField.setAccessible(true);
        subIdField.set(category, UUID.randomUUID());

        productId = UUID.randomUUID();
        product = Product.createProduct(store, category, "원래상품", "원래설명", "원래상세", 1000, true);

        // 리플렉션으로 ID 주입
        Field productIdField = Product.class.getDeclaredField("id");
        productIdField.setAccessible(true);
        productIdField.set(product, productId);

        optionGroupId = UUID.randomUUID();
        optionGroup = ProductOptionGroup.createOptionGroup(product, "사이즈");

        Field ogId = ProductOptionGroup.class.getDeclaredField("id");
        ogId.setAccessible(true);
        ogId.set(optionGroup, optionGroupId);
    }

    @DisplayName("상품 생성 성공 테스트 - 옵션 없이")
    @Test
    void createProduct_SuccessTest_NoOption() {
        // given
        UUID categoryId = UUID.randomUUID();
        when(memberUtil.getCurrentMember()).thenReturn(member);
        when(storeRepository.findByMember(member.memberId())).thenReturn(Optional.of(store));
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));

        ProductCreateRequest request =
                new ProductCreateRequest(
                        "상품1", "간단한 설명", "상세 설명", true, 1000, categoryId, null // 옵션 그룹 없음
                        );

        // when
        productService.createProduct(request);

        // then
        verify(productRepository, times(1)).save(any(Product.class));
        verify(storeRepository, times(1)).findByMember(member.memberId());
        verify(categoryRepository, times(1)).findById(categoryId);
    }

    @DisplayName("상품 생성 성공 테스트 - 옵션 그룹 포함")
    @Test
    void createProduct_SuccessTest_WithOptionGroup() {
        // given
        UUID categoryId = UUID.randomUUID();
        when(memberUtil.getCurrentMember()).thenReturn(member);
        when(storeRepository.findByMember(member.memberId())).thenReturn(Optional.of(store));
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));

        // 옵션 값 생성
        List<ProductOptionValueRequest> optionValues =
                List.of(
                        new ProductOptionValueRequest("색상: 빨강", 10, 0),
                        new ProductOptionValueRequest("색상: 파랑", 5, 0));

        // 옵션 그룹 생성
        List<ProductOptionGroupRequest> optionGroups =
                List.of(new ProductOptionGroupRequest("색상", optionValues));

        ProductCreateRequest request =
                new ProductCreateRequest(
                        "상품1", "간단한 설명", "상세 설명", true, 1000, categoryId, optionGroups // 옵션 그룹 없음
                        );

        // when
        productService.createProduct(request);

        // then
        verify(productRepository, times(1)).save(any(Product.class));
        verify(storeRepository, times(1)).findByMember(member.memberId());
        verify(categoryRepository, times(1)).findById(categoryId);
    }

    @DisplayName("상품 생성 실패 테스트 - 카테고리 없음")
    @Test
    void createProduct_FailTest_NoCategory() {
        // given
        UUID invalidCategoryId = UUID.randomUUID();

        when(memberUtil.getCurrentMember()).thenReturn(member);
        when(storeRepository.findByMember(member.memberId())).thenReturn(Optional.of(store));
        when(categoryRepository.findById(invalidCategoryId)).thenReturn(Optional.empty());

        ProductCreateRequest request =
                new ProductCreateRequest(
                        "상품1", "간단한 설명", "상세 설명", true, 1000, invalidCategoryId, null // 옵션 그룹 없음
                        );

        // when & then
        assertThatThrownBy(() -> productService.createProduct(request))
                .isInstanceOf(CommonException.class)
                .hasMessageContaining("카테고리 정보를 찾을 수 없습니다.");

        verify(categoryRepository, times(1)).findById(invalidCategoryId);
        verify(productRepository, never()).save(any());
    }

    @DisplayName("상품 생성 실패 테스트 - 상점 없음")
    @Test
    void createProduct_FailTest_NoStore() {
        // given
        UUID categoryId = UUID.randomUUID();
        when(memberUtil.getCurrentMember()).thenReturn(member);
        when(storeRepository.findByMember(member.memberId())).thenReturn(Optional.empty());

        ProductCreateRequest request =
                new ProductCreateRequest("상품1", "설명", "상세 설명", true, 12000, categoryId, null);

        // when & then
        assertThatThrownBy(() -> productService.createProduct(request))
                .isInstanceOf(CommonException.class)
                .hasMessageContaining("해당 상점을 찾을 수 없습니다.");

        verify(storeRepository, times(1)).findByMember(member.memberId());
        verify(productRepository, never()).save(any());
    }

    @DisplayName("상품 수정 성공 테스트")
    @Test
    void updateProduct_SuccessTest() throws Exception {
        // given
        when(productRepository.findById(any())).thenReturn(Optional.of(product));

        ProductUpdateRequest request = new ProductUpdateRequest("새상품명", "새설명", "새상세", false, 2000);

        // when
        productService.updateProduct(productId, request);

        // then
        assertThat(product.getId()).isNotNull();
        assertThat(product.getName()).isEqualTo("새상품명");
        assertThat(product.getDescription()).isEqualTo("새설명");
        assertThat(product.getDetailDescription()).isEqualTo("새상세");
        assertThat(product.getPrice()).isEqualTo(2000);
        assertThat(product.isPublic()).isFalse();

        verify(productRepository, times(1)).findById(productId);
        verify(memberUtil, times(1)).assertMemberResourceAccess(product.getStore().getMember());
    }

    @DisplayName("상품 수정 실패 테스트 - 변경된 정보 없음")
    @Test
    void updateProduct_FailTest_NoChanges() throws Exception {
        // given
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        ProductUpdateRequest request = new ProductUpdateRequest(null, null, null, null, null);

        // when & then
        assertThatThrownBy(() -> productService.updateProduct(productId, request))
                .isInstanceOf(CommonException.class)
                .hasMessageContaining("상품 수정에 대한 변경된 내용이 없습니다.");

        verify(productRepository, times(1)).findById(productId);
    }

    @DisplayName("상품 수정 실패 테스트 - 상품 없음")
    @Test
    void updateProduct_FailTest_ProductNotFound() throws Exception {
        // given
        UUID invalidProductId = UUID.randomUUID();

        when(productRepository.findById(invalidProductId)).thenReturn(Optional.empty());

        ProductUpdateRequest request = new ProductUpdateRequest("새상품명", "새설명", "새상세", false, 2000);

        // when & then
        assertThatThrownBy(() -> productService.updateProduct(invalidProductId, request))
                .isInstanceOf(CommonException.class)
                .hasMessageContaining("상품 정보를 찾을 수 없습니다.");

        verify(productRepository, times(1)).findById(invalidProductId);
    }

    //    @DisplayName("상품 카테고리 수정 성공 테스트")
    //    @Test
    //    void updateProductCategory_Success() throws Exception {
    //        // given
    //
    //        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
    //
    //
    // when(categoryRepository.findById(category.getCategoryId())).thenReturn(Optional.of(category));
    //
    //        UUID newCategoryId = UUID.randomUUID();
    //        ProductCategoryUpdateRequest request = new
    // ProductCategoryUpdateRequest(newCategoryId);
    //
    //        // when
    //        productService.updateProductCategory(productId, request);
    //
    //        // then
    //        assertThat(product.getCategory().getCategoryId()).isEqualTo(newCategoryId);
    //
    //        verify(productRepository, times(1)).findById(productId);
    //        verify(categoryRepository, times(1)).findById(newCategoryId);
    //        verify(memberUtil, times(1))
    //                .assertMemberResourceAccess(product.getStore().getMember());
    //    }
    @DisplayName("상품 삭제 성공 테스트")
    @Test
    void deleteProduct_Success() {
        // given
        when(memberUtil.getCurrentMember()).thenReturn(member);
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        // when
        productService.deleteProduct(productId);

        // then
        verify(memberUtil, times(1)).getCurrentMember();
        verify(productRepository, times(1)).findById(productId);
        verify(memberUtil, times(1)).assertMemberResourceAccess(product.getStore().getMember());
        verify(eventPublisher, times(1)).publishEvent(any(ProductDeletedEvent.class));
    }

    @DisplayName("상품 삭제 실패 테스트 - 상품 없음")
    @Test
    void deleteProduct_Fail_ProductNotFound() {
        // given
        when(memberUtil.getCurrentMember()).thenReturn(member);
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> productService.deleteProduct(productId))
                .isInstanceOf(CommonException.class);

        verify(memberUtil, times(1)).getCurrentMember();
        verify(productRepository, times(1)).findById(productId);
        verify(eventPublisher, never()).publishEvent(any());
    }

    @DisplayName("상점 기준 상품 일괄 삭제 성공 테스트 - 상품 존재")
    @Test
    void deleteProductsByStoreId_Success_WithProducts() throws Exception {
        // given
        Long deletedBy = member.memberId();

        // 기존 product는 setUp()에서 필드에 저장했다고 가정
        // product2 생성
        Product product2 =
                Product.createProduct(store, category, "두번째상품", "설명2", "상세2", 2000, true);

        // reflection ID 주입
        Field idField = Product.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(product2, UUID.randomUUID());

        // 상품 2개 mock 반환
        when(productRepository.findByStoreId(storeId)).thenReturn(List.of(product, product2));

        // when
        productService.deleteProductsByStoreId(storeId, deletedBy);

        // then
        verify(productRepository, times(1)).findByStoreId(storeId);

        // 이벤트는 두 상품 각각 1번씩 = 총 2회 발행됨
        verify(eventPublisher, times(2)).publishEvent(any(ProductDeletedEvent.class));
    }

    @DisplayName("상품 목록 조회 - 키워드 검색")
    @Test
    void getProductList_KeywordOnly() {
        String keyword = "바디워시";
        int size = 10;

        when(productRepository.findProductsByKeyword(null, size, keyword)).thenReturn(List.of());

        productService.getProductList(null, null, size, keyword);

        verify(productRepository, times(1)).findProductsByKeyword(null, size, keyword);
    }

    @DisplayName("상품 목록 조회 - 카테고리 검색")
    @Test
    void getProductList_CategoryOnly() {
        UUID categoryId = UUID.randomUUID();
        int size = 10;

        when(categoryRepository.findChildrenByParentId(any())).thenReturn(List.of());

        when(productRepository.findProductsByCategoryIds(null, size, List.of(categoryId)))
                .thenReturn(List.of());

        productService.getProductList(categoryId, null, size, null);

        verify(productRepository, times(1))
                .findProductsByCategoryIds(null, size, List.of(categoryId));
    }

    @DisplayName("상품 목록 조회 - 카테고리 + 키워드 검색")
    @Test
    void getProductList_CategoryAndKeyword() {
        UUID categoryId = UUID.randomUUID();
        UUID cursor = null;
        String keyword = "로션";
        int size = 10;

        // 하위 카테고리 목록 모킹
        when(categoryRepository.findChildrenByParentId(any()))
                .thenReturn(List.of()); // leaf category

        when(productRepository.findProductsByCategoryIdsAndKeyword(
                        cursor, size, List.of(categoryId), keyword))
                .thenReturn(List.of());

        // when
        productService.getProductList(categoryId, cursor, size, keyword);

        // then
        verify(productRepository, times(1))
                .findProductsByCategoryIdsAndKeyword(cursor, size, List.of(categoryId), keyword);
    }

    @DisplayName("상품 목록 조회 - 기본 전체 조회")
    @Test
    void getProductList_DefaultCursorPaging() {
        int size = 10;

        when(productRepository.findProductsByCursor(null, size)).thenReturn(List.of());

        productService.getProductList(null, null, size, null);

        verify(productRepository, times(1)).findProductsByCursor(null, size);
    }

    @DisplayName("옵션 그룹 생성 성공 테스트 - 옵션 값 없음")
    @Test
    void createOptionGroup_Success_NoOptionValues() {
        // given
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        doNothing().when(memberUtil).assertMemberResourceAccess(product.getStore().getMember());

        ProductOptionGroupRequest request = new ProductOptionGroupRequest("색상", null);

        // when
        productService.createOptionGroup(productId, request);

        // then
        assertThat(product.getOptionGroups()).hasSize(1);
        assertThat(product.getOptionGroups().get(0).getName()).isEqualTo("색상");

        verify(productRepository, times(1)).findById(productId);
        verify(productRepository, times(1)).save(product);
        verify(memberUtil, times(1)).assertMemberResourceAccess(product.getStore().getMember());
    }

    @DisplayName("옵션 그룹 생성 성공 테스트 - 옵션 값 포함")
    @Test
    void createOptionGroup_Success_WithOptionValues() {
        // given
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        doNothing().when(memberUtil).assertMemberResourceAccess(product.getStore().getMember());

        List<ProductOptionValueRequest> optionValues =
                List.of(
                        new ProductOptionValueRequest("빨강", 10, 0),
                        new ProductOptionValueRequest("파랑", 5, 500));

        ProductOptionGroupRequest request = new ProductOptionGroupRequest("색상", optionValues);

        // when
        productService.createOptionGroup(productId, request);

        // then
        assertThat(product.getOptionGroups()).hasSize(1);
        var group = product.getOptionGroups().get(0);

        assertThat(group.getName()).isEqualTo("색상");
        assertThat(group.getOptionValues()).hasSize(2);
        assertThat(group.getOptionValues().get(0).getName()).isEqualTo("빨강");
        assertThat(group.getOptionValues().get(1).getName()).isEqualTo("파랑");

        verify(productRepository, times(1)).save(product);
    }

    @DisplayName("옵션 그룹 생성 실패 테스트 - 상품 없음")
    @Test
    void createOptionGroup_Fail_ProductNotFound() {
        // given
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        ProductOptionGroupRequest request = new ProductOptionGroupRequest("색상", null);

        // when & then
        assertThatThrownBy(() -> productService.createOptionGroup(productId, request))
                .isInstanceOf(CommonException.class)
                .hasMessageContaining("상품 정보를 찾을 수 없습니다.");

        verify(productRepository, times(1)).findById(productId);
        verify(productRepository, never()).save(any());
    }

    @DisplayName("옵션 값 생성 성공 테스트")
    @Test
    void createOptionValue_Success() throws Exception {
        // given
        when(optionGroupRepository.findById(optionGroupId)).thenReturn(Optional.of(optionGroup));

        ProductOptionValueRequest request = new ProductOptionValueRequest("Large", 10, 500);

        // when
        productService.createOptionValue(optionGroup.getId(), request);

        // then
        assertThat(optionGroup.getOptionValues()).hasSize(1);
        assertThat(optionGroup.getOptionValues().get(0).getName()).isEqualTo("Large");
        assertThat(optionGroup.getOptionValues().get(0).getStockQuantity()).isEqualTo(10);
        assertThat(optionGroup.getOptionValues().get(0).getExtraPrice()).isEqualTo(500);

        verify(optionGroupRepository, times(1)).findById(optionGroupId);
        verify(optionGroupRepository, times(1)).save(optionGroup);
    }

    @DisplayName("옵션 값 생성 실패 테스트 - 옵션 그룹 없음")
    @Test
    void createOptionValue_Fail_OptionGroupNotFound() {
        // given
        when(optionGroupRepository.findById(optionGroupId)).thenReturn(Optional.empty());

        ProductOptionValueRequest request = new ProductOptionValueRequest("Large", 10, 500);

        // when & then
        assertThatThrownBy(() -> productService.createOptionValue(optionGroupId, request))
                .isInstanceOf(CommonException.class)
                .hasMessageContaining("옵션 그룹을 찾을 수 없습니다");

        verify(optionGroupRepository, times(1)).findById(optionGroupId);
        verify(optionGroupRepository, never()).save(any());
    }
}
