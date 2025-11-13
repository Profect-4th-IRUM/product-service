package com.irum.productservice.domain.discount.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.irum.global.advice.exception.CommonException;
import com.irum.openfeign.member.dto.response.MemberDto;
import com.irum.openfeign.member.enums.Role;
import com.irum.productservice.domain.category.domain.entity.Category;
import com.irum.productservice.domain.category.domain.repository.CategoryRepository;
import com.irum.productservice.domain.discount.domain.entity.Discount;
import com.irum.productservice.domain.discount.domain.repository.DiscountRepository;
import com.irum.productservice.domain.discount.dto.request.DiscountInfoUpdateRequest;
import com.irum.productservice.domain.discount.dto.request.DiscountRegisterRequest;
import com.irum.productservice.domain.discount.dto.response.DiscountInfoListResponse;
import com.irum.productservice.domain.discount.dto.response.DiscountInfoResponse;
import com.irum.productservice.domain.product.domain.entity.Product;
import com.irum.productservice.domain.product.domain.repository.ProductRepository;
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

@ExtendWith(MockitoExtension.class)
class DiscountServiceTest {

    @InjectMocks private DiscountService discountService;

    @Mock private DiscountRepository discountRepository;
    @Mock private ProductRepository productRepository;
    @Mock private StoreRepository storeRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private MemberUtil memberUtil;

    private MemberDto member;
    private Product product;
    private Store store;
    private UUID productId;
    private UUID storeId;
    private Category category;

    @BeforeEach
    void setUp() throws Exception {
        // Member Mock
        member = new MemberDto(1L, "이삭", "isak@test.com", "010-1111-1111", Role.OWNER);

        // Store Mock
        storeId = UUID.randomUUID();
        store =
                Store.createStore(
                        "상점1",
                        "010-1111-2222",
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

        // Product Mock
        productId = UUID.randomUUID();
        product =
                Product.createProduct(
                        store, // leaf category (최하위)
                        category, "상품1", "설명1", "상품 설명2", 1000, true);

        Field productIdField = Product.class.getDeclaredField("id");
        productIdField.setAccessible(true);
        productIdField.set(product, productId);
    }

    @DisplayName("할인 생성 성공 테스트")
    @Test
    void createDiscount_SuccessTest() {
        // given
        DiscountRegisterRequest request = new DiscountRegisterRequest("여름할인", 20, productId);

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(discountRepository.existsByProductId(productId)).thenReturn(false);

        // when
        discountService.createDiscount(request);

        // then
        verify(discountRepository, times(1)).save(any(Discount.class));
        verify(discountRepository, times(1)).existsByProductId(productId);
        verify(productRepository, times(1)).findById(productId);
    }

    @DisplayName("중복 할인 등록 시 예외 발생 테스트")
    @Test
    void createDiscount_Fail_DuplicateDiscount() {
        // given
        DiscountRegisterRequest request = new DiscountRegisterRequest("겨울할인", 30, productId);

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(discountRepository.existsByProductId(productId)).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> discountService.createDiscount(request))
                .isInstanceOf(CommonException.class)
                .hasMessageContaining("동일한 상품에 대한 할인은 중복 생성될 수 없습니다.");

        verify(discountRepository, times(1)).existsByProductId(productId);
        verify(discountRepository, never()).save(any(Discount.class));
    }

    @DisplayName("상품별 할인 정보 조회 성공 테스트")
    @Test
    void findDiscountInfoByProduct_SuccessTest() {
        // given
        UUID discountId = UUID.randomUUID();
        Discount discount = Discount.create("겨울세일", 30, product);

        // 리플렉션으로 ID 주입
        try {
            Field idField = Discount.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(discount, discountId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        when(discountRepository.findByProductId(productId)).thenReturn(Optional.of(discount));
        // 실제 서비스 내부에서 멤버 접근 검증용 호출
        lenient().doNothing().when(memberUtil).assertMemberResourceAccess(anyLong());

        // when
        var response = discountService.findDiscountInfoByProduct(productId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.discountId()).isEqualTo(discountId);
        assertThat(response.name()).isEqualTo("겨울세일");
        assertThat(response.amount()).isEqualTo(30);

        verify(discountRepository, times(1)).findByProductId(productId);
    }

    @DisplayName("상품별 할인 정보 조회 실패 테스트 - 할인 정보 없음")
    @Test
    void findDiscountInfoByProduct_FailTest_NotFound() {
        // given
        when(discountRepository.findByProductId(productId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> discountService.findDiscountInfoByProduct(productId))
                .isInstanceOf(CommonException.class)
                .hasMessageContaining("할인 정보를 찾을 수 없습니다"); // 실제 DiscountErrorCode 메시지에 맞춰 수정

        verify(discountRepository, times(1)).findByProductId(productId);
    }

    @DisplayName("상점별 할인 목록 조회 성공 테스트")
    @Test
    void findDiscountInfoListByStore_SuccessTest() {
        // given
        UUID cursor = UUID.randomUUID();
        UUID discountId1 = UUID.randomUUID();
        UUID discountId2 = UUID.randomUUID();

        // Mock 상점
        when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
        lenient().doNothing().when(memberUtil).assertMemberResourceAccess(anyLong());

        // Mock 할인 리스트 (2개)
        List<DiscountInfoResponse> discountList =
                List.of(
                        new DiscountInfoResponse(discountId1, "여름할인", 10, UUID.randomUUID()),
                        new DiscountInfoResponse(discountId2, "겨울할인", 20, UUID.randomUUID()));
        when(discountRepository.findDiscountListByCursor(storeId, cursor, 11))
                .thenReturn(discountList);

        // when
        DiscountInfoListResponse response =
                discountService.findDiscountInfoListByStore(storeId, cursor, 10);

        // then
        assertThat(response).isNotNull();
        assertThat(response.discountInfoList()).hasSize(2);
        assertThat(response.hasNext()).isFalse();
        assertThat(response.nextCursor()).isEqualTo(discountId2);

        verify(storeRepository, times(1)).findById(storeId);
        verify(discountRepository, times(1)).findDiscountListByCursor(storeId, cursor, 11);
    }

    @DisplayName("할인 정보 수정 성공 테스트")
    @Test
    void changeDiscountInfo_SuccessTest() throws Exception {
        // given
        UUID discountId = UUID.randomUUID();

        // 기존 할인 객체
        Discount discount = Discount.create("봄맞이 세일", 10, product);

        // 리플렉션으로 ID 주입
        Field idField = Discount.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(discount, discountId);

        // Mock 설정
        when(discountRepository.findById(discountId)).thenReturn(Optional.of(discount));
        lenient().doNothing().when(memberUtil).assertMemberResourceAccess(anyLong());

        // 수정 요청 DTO
        var request = new DiscountInfoUpdateRequest("여름맞이 세일", 25);

        // when
        discountService.changeDiscountInfo(discountId, request);

        // then
        assertThat(discount.getName()).isEqualTo("여름맞이 세일");
        assertThat(discount.getAmount()).isEqualTo(25);
        verify(discountRepository, times(1)).findById(discountId);
    }

    @DisplayName("할인 직접 삭제 성공 테스트")
    @Test
    void removeDiscount_SuccessTest() throws Exception {
        // given
        UUID discountId = UUID.randomUUID();

        // 기존 할인 생성
        Discount discount = Discount.create("봄맞이 세일", 10, product);

        // Reflection으로 ID 주입
        Field idField = Discount.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(discount, discountId);

        when(discountRepository.findById(discountId)).thenReturn(Optional.of(discount));
        when(memberUtil.getCurrentMember()).thenReturn(member);
        lenient().doNothing().when(memberUtil).assertMemberResourceAccess(anyLong());

        // when
        discountService.removeDiscount(discountId);

        // then
        assertThat(discount.getDeletedAt()).isNotNull();
        assertThat(discount.getDeletedBy()).isEqualTo(member.memberId());

        verify(discountRepository, times(1)).findById(discountId);
        verify(memberUtil, times(1)).getCurrentMember();
    }

    @DisplayName("상품 기준 할인 삭제 성공 테스트")
    @Test
    void deleteDiscountByProductId_SuccessTest() {
        // given
        UUID productId = UUID.randomUUID();
        Long deletedBy = 1L;

        Discount discount = Discount.create("겨울세일", 30, product);

        when(discountRepository.findByProductId(productId)).thenReturn(Optional.of(discount));

        // when
        discountService.deleteDiscountByProductId(productId, deletedBy);

        // then
        assertThat(discount.getDeletedAt()).isNotNull();
        assertThat(discount.getDeletedBy()).isEqualTo(deletedBy);
        verify(discountRepository, times(1)).findByProductId(productId);
    }

    @DisplayName("상품 기준 할인 삭제 실패 테스트 - 할인 없음")
    @Test
    void deleteDiscountByProductId_FailTest_NotFound() {
        // given
        UUID productId = UUID.randomUUID();

        when(discountRepository.findByProductId(productId)).thenReturn(Optional.empty());

        // when
        discountService.deleteDiscountByProductId(productId, 1L);

        // then
        verify(discountRepository, times(1)).findByProductId(productId);
    }
}
