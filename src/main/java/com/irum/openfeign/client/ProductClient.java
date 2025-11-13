package com.irum.openfeign.client;

import com.irum.openfeign.dto.response.ProductDto;
import com.irum.openfeign.dto.response.StoreDto;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "PRODUCT-SERVICE")
public interface ProductClient {

    // (상품ID 활용)상품, 상품옵션, 카테고리
    @GetMapping("/internal/products/{productId}")
    ProductDto getProduct(@PathVariable("productId") UUID productId);

    // (옵션ID 활용)상품, 상품옵션, 카테고리
    @GetMapping("/internal/products/options/{optionId}")
    ProductDto getProductByOption(@PathVariable("optionId") UUID optionId);

    // 상점 + 배송비 정책
    @GetMapping("/internal/stores/{storeId}")
    StoreDto getStore(@PathVariable("storeId") String storeId);
}
