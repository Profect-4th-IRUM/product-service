package com.irum.productservice.domain.product.Internal.service;

import com.irum.productservice.domain.discount.domain.entity.Discount;
import com.irum.productservice.domain.discount.domain.repository.DiscountRepository;
import com.irum.productservice.domain.product.domain.entity.Product;
import com.irum.productservice.domain.product.domain.entity.ProductOptionValue;
import com.irum.productservice.domain.product.domain.repository.ProductOptionValueRepository;
import com.irum.productservice.domain.product.domain.repository.ProductRepository;
import com.irum.productservice.domain.store.domain.entity.Store;
import com.irum.productservice.domain.store.domain.repository.StoreRepository;
import com.irum.productservice.domain.store.service.StoreService;
import com.irum.productservice.global.presentation.advice.exception.CommonException;
import com.irum.productservice.global.presentation.advice.exception.errorcode.ProductErrorCode;
import com.irum.productservice.global.presentation.advice.exception.errorcode.StoreErrorCode;
import com.irum.productservice.openfeign.dto.request.DeliveryPolicyWithProductRequest;
import com.irum.productservice.openfeign.dto.response.DeliveryPolicyWithProductDto;
import com.irum.productservice.openfeign.dto.response.ProductDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

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

    //상품 ID를 가지고 상품, 옵션(전체), 할인 조회
    public ProductDto getProduct(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new CommonException(ProductErrorCode.PRODUCT_NOT_FOUND));

        Discount discount = discountRepository.findByProductId(product.getId()).orElse(null);
        List<ProductOptionValue> optionValues =
                productOptionValueRepository.findAllByOptionGroup_Product(product);

        return ProductDto.from(product, optionValues, discount);
    }

    //옵션 ID를 가지고 상품, 옵션(전체), 할인 조회
    public ProductDto getProductByOption(UUID optionId) {
        ProductOptionValue optionValue = productOptionValueRepository.findById(optionId)
                .orElseThrow(() -> new CommonException(ProductErrorCode.PRODUCT_OPTION_VALUE_NOT_FOUND));

        Product product = optionValue.getOptionGroup().getProduct();
        Discount discount = discountRepository.findByProductId(product.getId()).orElse(null);
        List<ProductOptionValue> options =
                productOptionValueRepository.findAllByOptionGroup(optionValue.getOptionGroup());

        return ProductDto.from(product, options, discount);
    }

    // storeId, optionValueIdList -> 배송 정책, 상품 정보 조회
    public DeliveryPolicyWithProductDto getDeliveryPolicyWithProduct(DeliveryPolicyWithProductRequest request) {
        Store store = storeRepository.findByIdWithDeliveryPolicy(request.storeId()).orElseThrow(
                () -> new CommonException(StoreErrorCode.STORE_NOT_FOUND)
        );
        List<ProductOptionValue> productOptionValueList = productOptionValueRepository.findAllByIdFetchProduct(request.optionValueIdList());
        List<UUID> productIdList = productOptionValueList.stream().map(pov -> pov.getOptionGroup().getProduct().getId()).toList();
        List<Discount> discountList = discountRepository.findAllByProductIds(productIdList);
        Map<UUID, Integer> discountMap = discountList.stream().collect(
                Collectors.toMap(Discount::getId, Discount::getAmount)
        );

        return DeliveryPolicyWithProductDto.from(store, productOptionValueList, discountMap);
    }
}
