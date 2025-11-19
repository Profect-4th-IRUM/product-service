package com.irum.productservice.domain.product.Internal.controller;

import com.irum.openfeign.product.dto.request.ProductInternalRequest;
import com.irum.openfeign.product.dto.request.RollbackStockRequest;
import com.irum.openfeign.product.dto.response.ProductDto;
import com.irum.openfeign.product.dto.response.ProductInternalResponse;
import com.irum.productservice.domain.product.Internal.service.ProductInternalService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/products")
@RequiredArgsConstructor
public class ProductInternalController {

    private final ProductInternalService productInternalService;

    @GetMapping("/{productId}")
    public ProductDto getProduct(@PathVariable UUID productId) {
        return productInternalService.getProduct(productId);
    }

    @GetMapping("/options/{optionId}")
    public ProductDto getProductByOption(@PathVariable UUID optionId) {
        return productInternalService.getProductByOption(optionId);
    }

    // storeId, optionValueIdList -> 재고 감소 및 배송 정책, 상품 정보 조회
    @PutMapping("/stock")
    public ProductInternalResponse updateStock(@RequestBody ProductInternalRequest request) {
        return productInternalService.updateStock(request);
    }

    @PutMapping("/rollback")
    public void rollbackStock(@RequestBody RollbackStockRequest request) {
        productInternalService.rollbackStock(request);
    }
}
