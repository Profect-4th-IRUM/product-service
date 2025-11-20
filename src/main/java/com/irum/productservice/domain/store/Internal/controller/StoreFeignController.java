package com.irum.productservice.domain.store.Internal.controller;

import com.irum.openfeign.product.dto.response.StoreDto;
import com.irum.openfeign.product.dto.response.StoreResponse;
import com.irum.productservice.domain.store.Internal.service.StoreInternalService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/stores")
@RequiredArgsConstructor
public class StoreFeignController {
    private final StoreInternalService storeInternalService;

    @GetMapping("/{storeId}")
    public StoreDto getStore(@PathVariable("storeId") UUID storeId) {
        return storeInternalService.getStore(storeId);
    }

    @GetMapping("/{storeId}/owner")
    public StoreResponse getStoreId(@PathVariable UUID storeId) {
        return storeInternalService.getStoreResponse(storeId);
    }
}
