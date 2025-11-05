package com.irum.productservice.domain.store.Internal.controller;

import com.irum.productservice.domain.store.Internal.service.StoreInternalService;
import com.irum.productservice.openfeign.dto.response.StoreDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;


@RestController
@RequestMapping("/internal/stores")
@RequiredArgsConstructor
public class StoreFeignController {
    private final StoreInternalService storeInternalService;

    @GetMapping("/{storeId}")
    public StoreDto getStore(@PathVariable("storeId") UUID storeId) {
        return storeInternalService.getStore(storeId);
    }

}
