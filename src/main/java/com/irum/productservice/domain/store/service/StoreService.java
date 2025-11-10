package com.irum.productservice.domain.store.service;

import com.irum.global.advice.exception.CommonException;
import com.irum.openfeign.member.dto.response.MemberDto;
import com.irum.productservice.domain.product.domain.repository.ProductRepository;
import com.irum.productservice.domain.product.dto.request.ProductCursorResponse;
import com.irum.productservice.domain.product.dto.response.ProductResponse;
import com.irum.productservice.domain.store.domain.entity.Store;
import com.irum.productservice.domain.store.domain.repository.StoreRepository;
import com.irum.productservice.domain.store.dto.request.StoreCreateRequest;
import com.irum.productservice.domain.store.dto.request.StoreUpdateRequest;
import com.irum.productservice.domain.store.dto.response.StoreCreateResponse;
import com.irum.productservice.domain.store.dto.response.StoreInfoResponse;
import com.irum.productservice.domain.store.dto.response.StoreListResponse;
import com.irum.productservice.global.exception.errorcode.StoreErrorCode;
import com.irum.productservice.global.util.MemberUtil;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class StoreService {

    private final StoreRepository storeRepository;
    private final ProductRepository productRepository;
    private final MemberUtil memberUtil;

    public StoreCreateResponse createStore(StoreCreateRequest request) {
        MemberDto member = memberUtil.getCurrentMember();

        validateMemberHasNoStore(member.memberId());
        validateBusinessNumber(request.businessRegistrationNumber());
        validateTelemarketingNumber(request.telemarketingRegistrationNumber());

        Store store =
                Store.createStore(
                        request.name(),
                        request.contact(),
                        request.address(),
                        request.businessRegistrationNumber(),
                        request.telemarketingRegistrationNumber(),
                        member.memberId());

        storeRepository.save(store);

        return new StoreCreateResponse(store.getId());
    }

    public void changeStore(UUID storeId, StoreUpdateRequest request) {
        Store store = getStoreById(storeId);

        memberUtil.assertMemberResourceAccess(store.getMember());

        store.updateBasicInfo(request.name(), request.contact(), request.address());
    }

    public void withdrawStore(UUID storeId) {
        Store store = getStoreById(storeId);
        MemberDto member = memberUtil.getCurrentMember();
        memberUtil.assertMemberResourceAccess(store.getMember());
        store.softDelete(member.memberId());
    }

    @Transactional(readOnly = true)
    public List<StoreListResponse> findStoreList(UUID cursor, Integer size) {
        if (size == null || (size != 10 && size != 30 && size != 50)) {
            size = 10;
        }
        return storeRepository.findStoresByCursor(cursor, size);
    }

    @Transactional(readOnly = true)
    public StoreInfoResponse findStoreInfo(UUID storeId) {
        Store store = getStoreById(storeId);
        return StoreInfoResponse.from(store);
    }

    public ProductCursorResponse getMyStoreProducts(UUID cursor, Integer size) {
        MemberDto member = memberUtil.getCurrentMember();
        Store store =
                storeRepository
                        .findByMember(member.memberId())
                        .orElseThrow(() -> new CommonException(StoreErrorCode.STORE_NOT_FOUND));

        return getProductsByStore(store.getId(), cursor, size);
    }

    public ProductCursorResponse getStoreProducts(UUID storeId, UUID cursor, Integer size) {
        return getProductsByStore(storeId, cursor, size);
    }

    private ProductCursorResponse getProductsByStore(UUID storeId, UUID cursor, Integer size) {
        if (size == null || (size != 10 && size != 30 && size != 50)) {
            size = 10;
        }

        List<ProductResponse> products =
                productRepository.findProductsByStoreWithCursor(storeId, cursor, size);

        return ProductCursorResponse.of(products);
    }

    // 본인 소유 상점 검증
    private void validateStoreOwner(Store store, MemberDto member) {
        if (!store.getMember().equals(member.memberId())) {
            throw new CommonException(StoreErrorCode.UNAUTHORIZED_STORE_ACCESS);
        }
    }

    // 1인 1상점 제한
    private void validateMemberHasNoStore(Long member) {
        if (storeRepository.existsByMember(member)) {
            throw new CommonException(StoreErrorCode.STORE_ALREADY_EXISTS);
        }
    }

    // 사업자등록번호 중복 체크
    private void validateBusinessNumber(String businessRegistrationNumber) {
        if (storeRepository.existsByBusinessRegistrationNumber(businessRegistrationNumber)) {
            throw new CommonException(StoreErrorCode.BUSINESS_NUMBER_DUPLICATED);
        }
    }

    // 통신판매업번호 중복 체크
    private void validateTelemarketingNumber(String telemarketingRegistrationNumber) {
        if (storeRepository.existsByTelemarketingRegistrationNumber(
                telemarketingRegistrationNumber)) {
            throw new CommonException(StoreErrorCode.TELEMARKETING_NUMBER_DUPLICATED);
        }
    }

    private Store getStoreById(UUID storeId) {
        return storeRepository
                .findById(storeId)
                .orElseThrow(() -> new CommonException(StoreErrorCode.STORE_NOT_FOUND));
    }
}
