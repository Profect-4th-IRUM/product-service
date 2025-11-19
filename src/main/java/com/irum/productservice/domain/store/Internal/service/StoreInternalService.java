package com.irum.productservice.domain.store.Internal.service;

import com.irum.global.advice.exception.CommonException;
import com.irum.openfeign.product.dto.response.StoreDto;
import com.irum.openfeign.product.dto.response.StoreResponse;
import com.irum.productservice.domain.store.domain.entity.Store;
import com.irum.productservice.domain.store.domain.repository.StoreRepository;
import com.irum.productservice.domain.store.mapper.StoreMapper;
import com.irum.productservice.global.exception.errorcode.StoreErrorCode;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class StoreInternalService {

    private final StoreRepository storeRepository;
    private final StoreMapper storeMapper;

    public StoreDto getStore(UUID storeId) {
        Store store = getStoreById(storeId);
        return storeMapper.toDto(store);
    }

    public StoreResponse getStoreResponse(UUID storeId) {
        Store store = getStoreById(storeId);
        return StoreResponse.builder().storeId(storeId).memberId(store.getMember()).build();
    }

    private Store getStoreById(UUID storeId) {
        return storeRepository
                .findById(storeId)
                .orElseThrow(() -> new CommonException(StoreErrorCode.STORE_NOT_FOUND));
    }
}
