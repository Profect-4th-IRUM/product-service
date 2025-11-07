package com.irum.productservice.domain.category.controller;

import com.irum.productservice.domain.category.dto.request.CategoryCreateRequest;
import com.irum.productservice.domain.category.dto.request.CategoryUpdateRequest;
import com.irum.productservice.domain.category.dto.response.CategoryInfoResponse;
import com.irum.productservice.domain.category.dto.response.CategoryResponse;
import com.irum.productservice.domain.category.service.CategoryService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public List<CategoryInfoResponse> getAllCategories(
            @RequestParam(required = false) UUID parentId) {
        if (parentId != null) {
            return categoryService.findByParentId(parentId);
        }
        return categoryService.findRootCategories();
    }

    @GetMapping("/{id}")
    public CategoryResponse getCategoryById(@PathVariable UUID id) {
        return categoryService.getCategoryById(id);
    }

    @GetMapping("/tree")
    public List<CategoryResponse> getCategoryTree() {
        return categoryService.findCategoryTree();
    }

    @PostMapping
    public CategoryResponse createCategory(@Valid @RequestBody CategoryCreateRequest request) {
        return categoryService.createCategory(request);
    }

    @PatchMapping("/{id}")
    public CategoryResponse updateCategory(
            @PathVariable UUID id, @Valid @RequestBody CategoryUpdateRequest request) {
        return categoryService.updateCategory(id, request);
    }

    @DeleteMapping("/{id}")
    public void deleteCategory(@PathVariable UUID id) {
        categoryService.deleteCategory(id);
    }
}
