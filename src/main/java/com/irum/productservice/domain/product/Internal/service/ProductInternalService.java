package com.irum.productservice.domain.product.Internal.service;

import com.irum.global.advice.exception.CommonException;
import com.irum.openfeign.dto.request.RollbackStockRequest;
import com.irum.openfeign.dto.request.UpdateStockRequest;
import com.irum.openfeign.dto.response.ProductDto;
import com.irum.openfeign.dto.response.UpdateStockDto;
import com.irum.productservice.domain.discount.domain.entity.Discount;
import com.irum.productservice.domain.discount.domain.repository.DiscountRepository;
import com.irum.productservice.domain.product.domain.entity.Product;
import com.irum.productservice.domain.product.domain.entity.ProductOptionValue;
import com.irum.productservice.domain.product.domain.repository.ProductOptionValueRepository;
import com.irum.productservice.domain.product.domain.repository.ProductRepository;
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
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class ProductInternalService {

    private final ProductRepository productRepository;
    private final ProductOptionValueRepository productOptionValueRepository;
    private final DiscountRepository discountRepository;
    private final StoreRepository storeRepository;
    private final ProductOptionValueRepository optionValueRepository;

    // 상품 ID를 가지고 상품, 옵션(전체), 할인 조회
    public ProductDto getProduct(UUID id) {
        Product product =
                productRepository
                        .findById(id)
                        .orElseThrow(() -> new CommonException(ProductErrorCode.PRODUCT_NOT_FOUND));

        Discount discount = discountRepository.findByProductId(product.getId()).orElse(null);
        List<ProductOptionValue> optionValues =
                productOptionValueRepository.findAllByOptionGroup_Product(product);

        return ProductDto.from(product, optionValues, discount);
    }

    // 옵션 ID를 가지고 상품, 옵션(전체), 할인 조회
    public ProductDto getProductByOption(UUID optionId) {
        ProductOptionValue optionValue =
                productOptionValueRepository
                        .findById(optionId)
                        .orElseThrow(
                                () ->
                                        new CommonException(
                                                ProductErrorCode.PRODUCT_OPTION_VALUE_NOT_FOUND));

        Product product = optionValue.getOptionGroup().getProduct();
        Discount discount = discountRepository.findByProductId(product.getId()).orElse(null);
        List<ProductOptionValue> options =
                productOptionValueRepository.findAllByOptionGroup(optionValue.getOptionGroup());

        return ProductDto.from(product, options, discount);
    }

    // storeId, optionValueIdList -> 재고 감소 및 배송 정책, 상품 정보 조회
    @Transactional
    public UpdateStockDto updateStock(UpdateStockRequest request) {
        Store store =
                storeRepository
                        .findByIdWithDeliveryPolicy(request.storeId())
                        .orElseThrow(() -> new CommonException(StoreErrorCode.STORE_NOT_FOUND));
        // 옵션 id 모으기
        List<UUID> productOptionValueIdList =
                request.optionValueList().stream()
                        .map(UpdateStockRequest.OptionValueRequest::optionValueId)
                        .toList();

        // 옵션 조회. product group, product, store도 fetch join
        List<ProductOptionValue> productOptionValueList =
                productOptionValueRepository.findAllByIdWithLockFetchJoin(productOptionValueIdList);
        Map<UUID, ProductOptionValue> povMap =
                productOptionValueList.stream()
                        .collect(
                                Collectors.toMap(
                                        ProductOptionValue::getId,
                                        productOptionValue -> productOptionValue));

        // 정합 점검 : 존재하지 않는 상품을 주문하고 있는지
        if (productOptionValueList.size() != request.optionValueList().size()) {
            throw new CommonException(ProductErrorCode.PRODUCT_NOT_FOUND);
        }
        // 재고 감소
        for (UpdateStockRequest.OptionValueRequest optionValueRequest : request.optionValueList()) {
            ProductOptionValue pov = povMap.get(optionValueRequest.optionValueId());

            // 해당 상점의 상품을 주문하고 있는지
            if (!pov.getOptionGroup().getProduct().getStore().getId().equals(store.getId())) {
                throw new CommonException(ProductErrorCode.PRODUCT_NOT_IN_STORE);
            }

            // 재고보다 요청 물품 개수가 많을때
            if (pov.getStockQuantity() < optionValueRequest.quantity()) {
                throw new CommonException(ProductErrorCode.PRODUCT_OUT_OF_STOCK);
            }

            pov.decreaseStock(optionValueRequest.quantity());
        }

        // 할인 조회
        List<UUID> productIdList =
                productOptionValueList.stream()
                        .map(pov -> pov.getOptionGroup().getProduct().getId())
                        .toList();
        List<Discount> discountList = discountRepository.findAllByProductIds(productIdList);
        Map<UUID, Integer> discountMap =
                discountList.stream()
                        .collect(Collectors.toMap(Discount::getId, Discount::getAmount));

        return UpdateStockDto.from(store, productOptionValueList, discountMap);
    }

    /** 주문에 포함된 모든 상품의 재고를 다시 늘립니다. */
    @Transactional
    public void rollbackStock(RollbackStockRequest request) {

        // 재고를 되돌릴 ProductOptionValue ID 목록 추출
        List<UUID> optionIds =
                request.optionValueList().stream()
                        .map(RollbackStockRequest.OptionValueRequest::optionValueId)
                        .distinct() // 중복 ID 제거
                        .toList();

        // 락 획득
        List<ProductOptionValue> options =
                productOptionValueRepository.findAllByIdInWithLock(optionIds);

        // <productOptionValueId , ProductOptionValue> 형태의 Map
        Map<UUID, ProductOptionValue> optionMap =
                options.stream().collect(Collectors.toMap(ProductOptionValue::getId, pov -> pov));

        // 재고 되돌리기
        for (RollbackStockRequest.OptionValueRequest opr : request.optionValueList()) {
            ProductOptionValue option = optionMap.get(opr.optionValueId());

            if (option == null) {
                throw new CommonException(ProductErrorCode.PRODUCT_NOT_FOUND);
            }

            // 재고 되돌리기
            option.increaseStock(opr.quantity());
        }
    }
}
