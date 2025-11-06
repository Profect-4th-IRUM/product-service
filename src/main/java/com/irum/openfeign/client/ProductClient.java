package com.irum.openfeign.client;


import com.irum.openfeign.dto.response.ProductDto;
import com.irum.openfeign.dto.response.StoreDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "PRODUCT-SERVICE")
public interface ProductClient {

    @GetMapping("/internal/products/{productId}")
    ProductDto getProduct(@PathVariable("productId") UUID productId);

    @GetMapping("/internal/products/options/{optionId}")
    ProductDto getProductByOption(@PathVariable("optionId") UUID optionId);

    @GetMapping("/internal/stores/{storeId}")
    StoreDto getStore(@PathVariable("storeId") String storeId);
}