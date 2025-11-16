package com.irum.productservice.domain.category.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.irum.global.advice.exception.CommonException;
import com.irum.openfeign.member.dto.response.MemberDto;
import com.irum.openfeign.member.enums.Role;
import com.irum.productservice.domain.category.domain.entity.Category;
import com.irum.productservice.domain.category.domain.repository.CategoryRepository;
import com.irum.productservice.domain.category.dto.request.CategoryCreateRequest;
import com.irum.productservice.domain.category.dto.request.CategoryUpdateRequest;
import com.irum.productservice.domain.category.dto.response.CategoryResponse;
import com.irum.productservice.global.exception.errorcode.CategoryErrorCode;
import com.irum.productservice.global.util.MemberUtil;
import java.lang.reflect.Field;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @InjectMocks private CategoryService categoryService;
    @Mock private CategoryRepository categoryRepository;
    @Mock private MemberUtil memberUtil;

    private UUID categoryId;
    private Category category;
    private MemberDto member;

    @BeforeEach
    void setUp() throws Exception {
        categoryId = UUID.randomUUID();
        category = Category.createRootCategory("음식");
        Field idField = Category.class.getDeclaredField("categoryId");
        idField.setAccessible(true);
        idField.set(category, categoryId);

        member = new MemberDto(1L, "이삭", "isak@test.com", "010-1111-1111", Role.OWNER);
    }

    @DisplayName("루트 카테고리 생성 성공 테스트")
    @Test
    void createRootCategory_SuccessTest() throws Exception {
        CategoryCreateRequest request = new CategoryCreateRequest("음식", null);
        when(categoryRepository.save(any(Category.class)))
                .thenAnswer(
                        (Answer<Category>)
                                invocation -> {
                                    Category c = invocation.getArgument(0);
                                    Field field = Category.class.getDeclaredField("categoryId");
                                    field.setAccessible(true);
                                    field.set(c, categoryId);
                                    return c;
                                });

        CategoryResponse response = categoryService.createCategory(request);

        assertThat(response).isNotNull();
        assertThat(response.categoryId()).isEqualTo(categoryId);
        verify(categoryRepository, times(1)).save(any(Category.class));
    }

    @DisplayName("서브 카테고리 생성 성공 테스트")
    @Test
    void createSubCategory_SuccessTest() throws Exception {
        UUID parentId = UUID.randomUUID();
        Category parent = Category.createRootCategory("음식");
        Field parentIdField = Category.class.getDeclaredField("categoryId");
        parentIdField.setAccessible(true);
        parentIdField.set(parent, parentId);

        CategoryCreateRequest request = new CategoryCreateRequest("한식", parentId);
        when(categoryRepository.findById(parentId)).thenReturn(Optional.of(parent));
        when(categoryRepository.save(any(Category.class)))
                .thenAnswer(
                        (Answer<Category>)
                                invocation -> {
                                    Category c = invocation.getArgument(0);
                                    Field field = Category.class.getDeclaredField("categoryId");
                                    field.setAccessible(true);
                                    field.set(c, categoryId);
                                    return c;
                                });

        CategoryResponse response = categoryService.createCategory(request);

        assertThat(response.name()).isEqualTo("한식");
        verify(categoryRepository, times(1)).findById(parentId);
        verify(categoryRepository, times(1)).save(any(Category.class));
    }

    @DisplayName("서브 카테고리 생성 실패 테스트 - 부모 없음")
    @Test
    void createSubCategory_FailTest_WhenParentNotFound() {
        UUID parentId = UUID.randomUUID();
        CategoryCreateRequest request = new CategoryCreateRequest("양식", parentId);
        when(categoryRepository.findById(parentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.createCategory(request))
                .isInstanceOf(CommonException.class)
                .hasMessageContaining(CategoryErrorCode.CATEGORY_NOT_FOUND.getMessage());

        verify(categoryRepository, times(1)).findById(parentId);
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @DisplayName("카테고리 이름 수정 성공 테스트")
    @Test
    void updateCategory_SuccessTest() {
        // given
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        CategoryUpdateRequest request = new CategoryUpdateRequest("디저트");

        // when
        CategoryResponse response = categoryService.updateCategory(categoryId, request);

        // then
        assertThat(category.getName()).isEqualTo("디저트");
        assertThat(response.name()).isEqualTo("디저트");
        verify(categoryRepository, times(1)).findById(categoryId);
    }

    @DisplayName("카테고리 수정 실패 테스트 - 존재하지 않음")
    @Test
    void updateCategory_FailTest_WhenNotFound() {
        // given
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());
        CategoryUpdateRequest request = new CategoryUpdateRequest("디저트");

        // when & then
        assertThatThrownBy(() -> categoryService.updateCategory(categoryId, request))
                .isInstanceOf(CommonException.class)
                .hasMessageContaining(CategoryErrorCode.CATEGORY_NOT_FOUND.getMessage());

        verify(categoryRepository, times(1)).findById(categoryId);
        verifyNoMoreInteractions(categoryRepository);
    }

    @DisplayName("카테고리 삭제 성공 테스트")
    @Test
    void deleteCategory_SuccessTest() {
        // given
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(memberUtil.getCurrentMember()).thenReturn(member);

        // when
        categoryService.deleteCategory(categoryId);

        // then
        assertThat(category.getDeletedAt()).isNotNull();
        verify(categoryRepository, times(1)).findById(categoryId);
        verify(memberUtil, times(1)).getCurrentMember();
    }

    @DisplayName("카테고리 삭제 실패 테스트 - 존재하지 않음")
    @Test
    void deleteCategory_FailTest_WhenNotFound() {
        // given
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> categoryService.deleteCategory(categoryId))
                .isInstanceOf(CommonException.class)
                .hasMessageContaining(CategoryErrorCode.CATEGORY_NOT_FOUND.getMessage());

        verify(categoryRepository, times(1)).findById(categoryId);
        verify(memberUtil, never()).getCurrentMember();
    }
}
