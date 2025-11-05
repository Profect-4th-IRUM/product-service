package com.irum.productservice.openfeign.client;


import com.irum.productservice.openfeign.dto.response.ProductDto;
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


}