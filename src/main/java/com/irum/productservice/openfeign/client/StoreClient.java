package com.irum.productservice.openfeign.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import com.irum.productservice.openfeign.dto.response.StoreDto;
@FeignClient(name = "PRODUCT-SERVICE")
public interface StoreClient {

    @GetMapping("/internal/stores/{storeId}")
    StoreDto getStore(@PathVariable("storeId") String storeId);
}
