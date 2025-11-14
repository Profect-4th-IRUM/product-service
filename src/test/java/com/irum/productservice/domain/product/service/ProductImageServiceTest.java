package com.irum.productservice.domain.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.irum.global.advice.exception.CommonException;
import com.irum.openfeign.member.dto.response.MemberDto;
import com.irum.productservice.domain.category.domain.entity.Category;
import com.irum.productservice.domain.category.domain.repository.CategoryRepository;
import com.irum.productservice.domain.product.domain.entity.Product;
import com.irum.productservice.domain.product.domain.entity.ProductImage;
import com.irum.productservice.domain.product.domain.repository.ProductImageRepository;
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
public class ProductImageServiceTest {

    @InjectMocks private ProductImageService productImageService;

    @Mock private FileStorageService fileStorageService;
    @Mock private ProductImageRepository productImageRepository;
    @Mock private ProductRepository productRepository;
    @Mock private MemberUtil memberUtil;
    @Mock private StoreRepository storeRepository;
    @Mock private CategoryRepository categoryRepository;

    private ProductImage productImage;
    private Product product;
    private UUID productId;
    private UUID imageId;
    private MemberDto member;
    private Store store;
    private UUID storeId;
    private Category category;
    private UUID categoryId;

    @BeforeEach
    public void setUp() throws Exception {
        member =
                new MemberDto(
                        1L,
                        "이삭",
                        "isak@naver.com",
                        "010-3030-4040",
                        com.irum.openfeign.member.enums.Role.OWNER);
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

        Field productIdField = Product.class.getDeclaredField("id");
        productIdField.setAccessible(true);
        productIdField.set(product, productId);
    }

    @DisplayName("상품 이미지 추가 성공 테스트 - 첫 이미지 default 설정")
    @Test
    void saveProductImages_Success_FirstImageDefault() {
        // given
        List<String> urls = List.of("https://s3.com/img1.png", "https://s3.com/img2.png");

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productImageRepository.existsByProductId(productId)).thenReturn(false); // 기존 이미지 없음

        // when
        productImageService.saveProductImages(productId, urls);

        // then
        verify(productRepository, times(1)).findById(productId);
        verify(memberUtil, times(1)).assertMemberResourceAccess(product.getStore().getMember());

        // 이미지 2건 저장
        verify(productImageRepository, times(2)).save(any(ProductImage.class));
    }

    @DisplayName("상품 이미지 추가 실패 테스트 - 상품 없음")
    @Test
    void saveProductImages_Fail_ProductNotFound() {
        // given
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        List<String> urls = List.of("https://s3.com/img1.png");

        // when & then
        assertThatThrownBy(() -> productImageService.saveProductImages(productId, urls))
                .isInstanceOf(CommonException.class)
                .hasMessageContaining("상품 정보를 찾을 수 없습니다");

        verify(productImageRepository, never()).save(any());
    }

    void changeDefaultImage_Success() throws Exception {

        // ----- fresh image list -----
        UUID imgId1 = UUID.randomUUID();
        UUID imgId2 = UUID.randomUUID();

        ProductImage img1 = ProductImage.create(product, "url1", true);
        ProductImage img2 = ProductImage.create(product, "url2", false);

        Field f1 = ProductImage.class.getDeclaredField("id");
        f1.setAccessible(true);
        f1.set(img1, imgId1);

        Field f2 = ProductImage.class.getDeclaredField("id");
        f2.setAccessible(true);
        f2.set(img2, imgId2);

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productImageRepository.findByProductId(productId)).thenReturn(List.of(img1, img2));

        // when
        productImageService.changeDefaultImage(productId, imgId2);

        // then
        assertThat(img1.isDefault()).isFalse(); // 기존 default → 해제
        assertThat(img2.isDefault()).isTrue(); // 새 default 지정

        verify(productRepository).findById(productId);
        verify(productImageRepository).findByProductId(productId);
        verify(memberUtil).assertMemberResourceAccess(product.getStore().getMember());
    }

    @DisplayName("대표 이미지 변경 실패 - 상품 없음")
    @Test
    void changeDefaultImage_Fail_NoProduct() {

        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        assertThatThrownBy(
                        () -> productImageService.changeDefaultImage(productId, UUID.randomUUID()))
                .isInstanceOf(CommonException.class);
    }

    @DisplayName("대표 이미지 변경 실패 - 잘못된 이미지 ID")
    @Test
    void changeDefaultImage_Fail_InvalidImage() throws Exception {

        UUID validImgId = UUID.randomUUID();
        ProductImage img1 = ProductImage.create(product, "url1", true);

        Field f1 = ProductImage.class.getDeclaredField("id");
        f1.setAccessible(true);
        f1.set(img1, validImgId);

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productImageRepository.findByProductId(productId)).thenReturn(List.of(img1));

        UUID wrongId = UUID.randomUUID();

        assertThatThrownBy(() -> productImageService.changeDefaultImage(productId, wrongId))
                .isInstanceOf(CommonException.class);
    }

    @DisplayName("상품 이미지 삭제 성공 테스트 - default 이미지 삭제 시 새로운 default 지정")
    @Test
    void deleteProductImage_Success_DefaultImage() throws Exception {

        when(memberUtil.getCurrentMember()).thenReturn(member);

        // ----- 기존 이미지 2개 생성 -----
        UUID imgId1 = UUID.randomUUID();
        UUID imgId2 = UUID.randomUUID();

        ProductImage img1 = ProductImage.create(product, "url1", true); // default
        ProductImage img2 = ProductImage.create(product, "url2", false);

        Field f1 = ProductImage.class.getDeclaredField("id");
        f1.setAccessible(true);
        f1.set(img1, imgId1);

        Field f2 = ProductImage.class.getDeclaredField("id");
        f2.setAccessible(true);
        f2.set(img2, imgId2);

        // ----- mock -----
        when(productImageRepository.findById(imgId1)).thenReturn(Optional.of(img1));
        when(productImageRepository.findTopByProductIdOrderByCreatedAtDesc(productId))
                .thenReturn(Optional.of(img2));

        // when
        productImageService.deleteProductImage(productId, imgId1);

        // then
        assertThat(img1.isDefault()).isFalse();
        assertThat(img2.isDefault()).isTrue(); // 새 default 지정

        verify(fileStorageService, times(1)).delete("url1");
        verify(memberUtil, times(1)).assertMemberResourceAccess(product.getStore().getMember());
    }

    @DisplayName("상품 이미지 삭제 실패 - 이미지가 해당 상품에 속하지 않음")
    @Test
    void deleteProductImage_Fail_InvalidRelation() throws Exception {

        UUID imgId = UUID.randomUUID();
        Product anotherProduct =
                Product.createProduct(store, category, "다른상품", "d", "d", 1000, true);

        Field aid = Product.class.getDeclaredField("id");
        aid.setAccessible(true);
        aid.set(anotherProduct, UUID.randomUUID());

        ProductImage wrongImage = ProductImage.create(anotherProduct, "urlX", true);
        Field iid = ProductImage.class.getDeclaredField("id");
        iid.setAccessible(true);
        iid.set(wrongImage, imgId);

        when(productImageRepository.findById(imgId)).thenReturn(Optional.of(wrongImage));

        assertThatThrownBy(() -> productImageService.deleteProductImage(productId, imgId))
                .isInstanceOf(CommonException.class)
                .hasMessageContaining("요청한 상품과 이미지가 일치하지 않습니다.");
    }

    @DisplayName("상품 기준 모든 이미지 삭제 성공 테스트")
    @Test
    void deleteProductImagesByProductId_Success() throws Exception {

        UUID imgId1 = UUID.randomUUID();
        UUID imgId2 = UUID.randomUUID();

        ProductImage img1 = ProductImage.create(product, "url1", true);
        ProductImage img2 = ProductImage.create(product, "url2", false);

        Field f1 = ProductImage.class.getDeclaredField("id");
        f1.setAccessible(true);
        f1.set(img1, imgId1);

        Field f2 = ProductImage.class.getDeclaredField("id");
        f2.setAccessible(true);
        f2.set(img2, imgId2);

        when(productImageRepository.findByProductId(productId)).thenReturn(List.of(img1, img2));

        Long deletedBy = 999L;

        // when
        productImageService.deleteProductImagesByProductId(productId, deletedBy);

        // then
        assertThat(img1.getDeletedBy()).isEqualTo(deletedBy);
        assertThat(img2.getDeletedBy()).isEqualTo(deletedBy);

        verify(productImageRepository, times(1)).findByProductId(productId);
    }

    @DisplayName("상품 기준 전체 이미지 삭제 - 이미지가 없는 경우")
    @Test
    void deleteProductImage_Fail_NoImage() {

        UUID fakeId = UUID.randomUUID();

        when(productImageRepository.findById(fakeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productImageService.deleteProductImage(productId, fakeId))
                .isInstanceOf(CommonException.class);
    }

    @DisplayName("상품 이미지 삭제 실패 - 이미지 없음")
    @Test
    void deleteProductImagesByProductId_NoImages() {

        when(productImageRepository.findByProductId(productId)).thenReturn(List.of());

        productImageService.deleteProductImagesByProductId(productId, 1L);

        verify(productImageRepository, times(1)).findByProductId(productId);
        verifyNoMoreInteractions(productImageRepository);
    }

    @DisplayName("상품 이미지 조회 성공 테스트 - 이미지 2개 조회")
    @Test
    void getProductImages_Success_TwoImages() throws Exception {

        // given
        UUID imgId1 = UUID.randomUUID();
        UUID imgId2 = UUID.randomUUID();

        ProductImage img1 = ProductImage.create(product, "url1", true);
        ProductImage img2 = ProductImage.create(product, "url2", false);

        // 리플렉션 ID 세팅
        Field id1 = ProductImage.class.getDeclaredField("id");
        id1.setAccessible(true);
        id1.set(img1, imgId1);

        Field id2 = ProductImage.class.getDeclaredField("id");
        id2.setAccessible(true);
        id2.set(img2, imgId2);

        when(productImageRepository.findByProductId(productId)).thenReturn(List.of(img1, img2));

        // when
        var response = productImageService.getProductImages(productId);

        // then
        assertThat(response).hasSize(2);
        assertThat(response.get(0).imageUrl()).isEqualTo("url1");
        assertThat(response.get(1).imageUrl()).isEqualTo("url2");

        verify(productImageRepository, times(1)).findByProductId(productId);
    }
}
