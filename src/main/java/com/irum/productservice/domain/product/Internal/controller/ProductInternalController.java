package com.irum.productservice.domain.product.Internal.controller;

import com.irum.productservice.domain.product.Internal.service.ProductInternalService;
import com.irum.productservice.openfeign.dto.request.DeliveryPolicyWithProductRequest;
import com.irum.productservice.openfeign.dto.response.DeliveryPolicyWithProductDto;
import com.irum.productservice.openfeign.dto.response.ProductDto;
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


    // storeId, optionValueIdList -> 배송 정책, 상품 정보 조회
    @GetMapping("/list")
    public DeliveryPolicyWithProductDto getDeliveryPolicyWithProduct(@RequestBody DeliveryPolicyWithProductRequest request) {
        return productInternalService.getDeliveryPolicyWithProduct(request);
    }
}
