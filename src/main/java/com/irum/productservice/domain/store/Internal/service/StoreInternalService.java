package com.irum.productservice.domain.store.Internal.service;

import com.irum.global.advice.exception.CommonException;
import com.irum.productservice.domain.store.domain.repository.StoreRepository;
import com.irum.productservice.domain.store.domain.entity.Store;
import com.irum.productservice.global.exception.errorcode.StoreErrorCode;
import com.irum.openfeign.dto.response.StoreDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class StoreInternalService {

    private final StoreRepository storeRepository;

    public StoreDto getStore(UUID storeId) {
        Store store = getStoreById(storeId);
        return StoreDto.from(store);
    }

    private Store getStoreById(UUID storeId) {
        return storeRepository
                .findById(storeId)
                .orElseThrow(() -> new CommonException(StoreErrorCode.STORE_NOT_FOUND));
    }
}
