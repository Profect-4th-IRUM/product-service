package com.irum.productservice.domain.store.controller;

import com.irum.productservice.domain.product.dto.request.ProductCursorResponse;
import com.irum.productservice.domain.store.dto.request.StoreCreateRequest;
import com.irum.productservice.domain.store.dto.request.StoreUpdateRequest;
import com.irum.productservice.domain.store.dto.response.StoreCreateResponse;
import com.irum.productservice.domain.store.dto.response.StoreInfoResponse;
import com.irum.productservice.domain.store.dto.response.StoreListResponse;
import com.irum.productservice.domain.store.service.StoreService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/stores")
@Slf4j
public class StoreController {

    private final StoreService storeService;

    @PostMapping
    public ResponseEntity<StoreCreateResponse> createStore(
            @Valid @RequestBody StoreCreateRequest request) {
        log.info("상점 생성 요청: {}", request);
        StoreCreateResponse response = storeService.createStore(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{storeId}")
    public ResponseEntity<Void> updateStore(
            @PathVariable UUID storeId, @Valid @RequestBody StoreUpdateRequest request) {
        log.info("상점 정보 수정 요청: storeId={}, request={}", storeId, request);
        storeService.changeStore(storeId, request);
        return ResponseEntity.noContent().build(); // 204 No Content
    }

    @DeleteMapping("/{storeId}")
    public ResponseEntity<Void> deleteStore(@PathVariable UUID storeId) {
        log.info("상점 삭제 요청 : storeId={}", storeId);
        storeService.withdrawStore(storeId);
        return ResponseEntity.noContent().build(); // 204 No Content
    }

    @GetMapping
    public ResponseEntity<List<StoreListResponse>> getStoreList(
            @RequestParam(required = false) UUID cursor,
            @RequestParam(required = false) Integer size) {
        log.info("상점 목록 조회 요청: cursor={}, size={}", cursor, size);
        List<StoreListResponse> response = storeService.findStoreList(cursor, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{storeId}")
    public ResponseEntity<StoreInfoResponse> getStoreDetail(@PathVariable UUID storeId) {
        log.info("상점 상세 조회 요청: storeId={}", storeId);
        StoreInfoResponse response = storeService.findStoreInfo(storeId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/products")
    public ResponseEntity<ProductCursorResponse> getMyStoreProducts(
            @RequestParam(required = false) UUID cursor,
            @RequestParam(required = false) Integer size) {
        log.info("내 상점 상품 목록 조회 요청: cursor={}, size={}", cursor, size);
        ProductCursorResponse response = storeService.getMyStoreProducts(cursor, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{storeId}/products")
    public ResponseEntity<ProductCursorResponse> getStoreProducts(
            @PathVariable UUID storeId,
            @RequestParam(required = false) UUID cursor,
            @RequestParam(required = false) Integer size) {
        log.info("상점 상품 목록 조회 요청: storeId={}", storeId);
        ProductCursorResponse response = storeService.getStoreProducts(storeId, cursor, size);
        return ResponseEntity.ok(response);
    }
}
