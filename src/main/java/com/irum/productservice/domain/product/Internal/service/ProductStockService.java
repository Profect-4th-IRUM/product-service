package com.irum.productservice.domain.product.Internal.service;

import com.irum.global.advice.exception.CommonException;
import com.irum.openfeign.product.dto.request.ProductInternalRequest;
import com.irum.openfeign.product.dto.request.RollbackStockRequest;
import com.irum.openfeign.product.dto.response.ProductInternalResponse;
import com.irum.productservice.domain.discount.domain.entity.Discount;
import com.irum.productservice.domain.discount.domain.repository.DiscountRepository;
import com.irum.productservice.domain.product.Internal.service.converter.ProductInternalResponseMapper;
import com.irum.productservice.domain.product.domain.entity.ProductOptionValue;
import com.irum.productservice.domain.product.domain.repository.ProductOptionValueRepository;
import com.irum.productservice.domain.store.domain.entity.Store;
import com.irum.productservice.domain.store.domain.repository.StoreRepository;
import com.irum.productservice.global.exception.errorcode.ProductErrorCode;
import com.irum.productservice.global.exception.errorcode.StoreErrorCode;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductStockService {
    private final ProductOptionValueRepository productOptionValueRepository;
    private final DiscountRepository discountRepository;
    private final StoreRepository storeRepository;

    @Transactional
    public ProductInternalResponse updateStockInTransaction(ProductInternalRequest request) {
        // 상점 + 배송정책 조회
        Store store =
                storeRepository
                        .findByIdWithDeliveryPolicy(request.storeId())
                        .orElseThrow(() -> new CommonException(StoreErrorCode.STORE_NOT_FOUND));
        // 옵션 id 모으기
        List<UUID> productOptionValueIdList =
                request.optionValueList().stream()
                        .map(ProductInternalRequest.OptionValueRequest::optionValueId)
                        .toList();

        // 옵션 조회. optionGroup, product, store 까지 fetch join
        List<ProductOptionValue> productOptionValueList =
                productOptionValueRepository.findAllByIdWithFetchJoin(productOptionValueIdList);
        Map<UUID, ProductOptionValue> povMap =
                productOptionValueList.stream()
                        .collect(
                                Collectors.toMap(
                                        ProductOptionValue::getId,
                                        productOptionValue -> productOptionValue));

        // 정합 점검 : 존재하지 않는 옵션이 섞여 있는지
        validateAllOptionValuesExist(request, productOptionValueList);

        // 재고 감소
        for (ProductInternalRequest.OptionValueRequest optionValueRequest : request.optionValueList()) {
            ProductOptionValue pov = povMap.get(optionValueRequest.optionValueId());

            // 주문 검증 : 상점의 상품인지, 재고 부족 체크
            validateStoreAndStock(pov, store, optionValueRequest);

            pov.decreaseStock(optionValueRequest.quantity());
        }

        // 할인 조회
        List<UUID> productIdList =
                productOptionValueList.stream()
                        .map(pov -> pov.getOptionGroup().getProduct().getId())
                        .toList();
        List<Discount> discountList = discountRepository.findAllByProductIds(productIdList);

        // productId 기준으로 할인 맵핑
        Map<UUID, Integer> discountMap =
                discountList.stream()
                        .collect(
                                Collectors.toMap(
                                        discount -> discount.getProduct().getId(), // productId
                                        Discount::getAmount));

        return ProductInternalResponseMapper.toProductInternalResponse(store, productOptionValueList, discountMap);
    }

    /** 모든 옵션이 존재하는지 확인 */
    private void validateAllOptionValuesExist(
            ProductInternalRequest request, List<ProductOptionValue> povList) {
        if (povList.size() != request.optionValueList().size()) {
            throw new CommonException(ProductErrorCode.PRODUCT_NOT_FOUND);
        }
    }

    /** 주문 검증 : 상점의 상품인지, 재고 부족 체크 */
    private void validateStoreAndStock(
            ProductOptionValue pov,
            Store store,
            ProductInternalRequest.OptionValueRequest optionValueRequest) {

        // 해당 상점의 상품인지 확인
        if (!pov.getOptionGroup().getProduct().getStore().getId().equals(store.getId())) {
            throw new CommonException(ProductErrorCode.PRODUCT_NOT_IN_STORE);
        }

        // 재고보다 요청 수량이 많은지 체크
        if (pov.getStockQuantity() < optionValueRequest.quantity()) {
            throw new CommonException(ProductErrorCode.PRODUCT_OUT_OF_STOCK);
        }
    }

    @Transactional
    public void rollbackStockInTransactional(RollbackStockRequest request) {

        // 재고를 되돌릴 ProductOptionValue ID 목록 추출
        List<UUID> optionIds =
                request.optionValueList().stream()
                        .map(RollbackStockRequest.OptionValueRequest::optionValueId)
                        .distinct()
                        .toList();

        List<ProductOptionValue> options = productOptionValueRepository.findAllByIds(optionIds);

        // <productOptionValueId , ProductOptionValue> 형태의 Map
        Map<UUID, ProductOptionValue> optionMap =
                options.stream().collect(Collectors.toMap(ProductOptionValue::getId, pov -> pov));

        // 재고 되돌리기
        for (RollbackStockRequest.OptionValueRequest opr : request.optionValueList()) {
            ProductOptionValue option = optionMap.get(opr.optionValueId());

            // 옵션 존재 여부 체크
            validateOptionExist(option);

            // 재고 되돌리기
            option.increaseStock(opr.quantity());
        }
    }

    /** option 존재하는지 체크 */
    private void validateOptionExist(ProductOptionValue option) {
        if (option == null) {
            throw new CommonException(ProductErrorCode.PRODUCT_NOT_FOUND);
        }
    }
}
