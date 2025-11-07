package com.irum.productservice.domain.deliverypolicy.service;

import com.irum.global.advice.exception.CommonException;
import com.irum.productservice.domain.deliverypolicy.domain.entity.DeliveryPolicy;
import com.irum.productservice.domain.deliverypolicy.domain.repository.DeliveryPolicyRepository;
import com.irum.productservice.domain.deliverypolicy.dto.request.DeliveryPolicyCreateRequest;
import com.irum.productservice.domain.deliverypolicy.dto.request.DeliveryPolicyInfoUpdateRequest;
import com.irum.productservice.domain.deliverypolicy.dto.response.DeliveryPolicyInfoResponse;
import com.irum.productservice.domain.product.domain.entity.Product;
import com.irum.productservice.domain.product.domain.repository.ProductRepository;
import com.irum.productservice.domain.store.domain.entity.Store;
import com.irum.productservice.domain.store.domain.repository.StoreRepository;
import com.irum.productservice.global.exception.errorcode.DeliveryPolicyErrorCode;
import com.irum.productservice.global.exception.errorcode.StoreErrorCode;
import com.irum.productservice.global.util.MemberUtil;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import openfeign.member.dto.response.MemberDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class DeliveryPolicyService {
    private final DeliveryPolicyRepository deliveryPolicyRepository;
    private final StoreRepository storeRepository;
    private final MemberUtil memberUtil;
    private final ProductRepository productRepository;

    public void createDeliveryPolicy(DeliveryPolicyCreateRequest request) {
        MemberDto member = memberUtil.getCurrentMember();
        Store store = validateAndGetStoreByMember(member);

        ensureStoreHasNoExistingPolicy(store);

        DeliveryPolicy deliveryPolicy =
                DeliveryPolicy.createPolicy(
                        request.defaultDeliveryFee(),
                        request.minQuantity(),
                        request.minAmount(),
                        store);

        deliveryPolicyRepository.save(deliveryPolicy);
    }

    public void changeDeliveryPolicy(
            UUID deliveryPolicyId, DeliveryPolicyInfoUpdateRequest request) {
        DeliveryPolicy deliveryPolicy = validDeliveryPolicy(deliveryPolicyId);

        if (request.defaultDeliveryFee() != null) {
            deliveryPolicy.updateFee(request.defaultDeliveryFee());
        }
        if (request.minQuantity() != null) {
            deliveryPolicy.updateQuantity(request.minQuantity());
        }
        if (request.minAmount() != null) {
            deliveryPolicy.updateAmount(request.minAmount());
        }
    }

    public void withdrawDeliveryPolicy(UUID deliveryPolicyId) {
        DeliveryPolicy deliveryPolicy = validDeliveryPolicy(deliveryPolicyId);
        MemberDto member = memberUtil.getCurrentMember();

        deliveryPolicy.softDelete(member.memberId());
    }

    public void deleteDeliveryPolicyByStoreId(UUID storeId, Long deletedBy) {
        deliveryPolicyRepository.findByStoreId(storeId)
                .ifPresent(policy -> policy.softDelete(deletedBy));
    }

    @Transactional(readOnly = true)
    public DeliveryPolicyInfoResponse findDeliveryPolicy(UUID deliveryPolicyId) {
        DeliveryPolicy deliveryPolicy = validDeliveryPolicy(deliveryPolicyId);
        return DeliveryPolicyInfoResponse.from(deliveryPolicy);
    }

    // 상점에 배달비 정책이 존재하는지 확인
    private void ensureStoreHasNoExistingPolicy(Store store) {
        if (deliveryPolicyRepository.existsByStore(store)) {
            throw new CommonException(DeliveryPolicyErrorCode.ALREADY_EXISTS);
        }
    }

    // 로그인된 멤버의 상점 조회
    private Store validateAndGetStoreByMember(MemberDto member) {
        return storeRepository
                .findByMember(member.memberId())
                .orElseThrow(() -> new CommonException(StoreErrorCode.STORE_NOT_FOUND));
    }

    // 본인 소유 검증.
    private DeliveryPolicy validDeliveryPolicy(UUID deliveryPolicyId) {
        DeliveryPolicy deliverypolicy =
                deliveryPolicyRepository
                        .findById(deliveryPolicyId)
                        .orElseThrow(
                                () ->
                                        new CommonException(
                                                DeliveryPolicyErrorCode.DELIVERY_POLICY_NOT_FOUND));
        memberUtil.assertMemberResourceAccess(deliverypolicy.getStore().getMember());
        return deliverypolicy;
    }
}

// 상점에 이미 정책이 존재하는지 (정책 생성을 위해)
// 상점의 존재 하는지
// 본인 소유 검증.
