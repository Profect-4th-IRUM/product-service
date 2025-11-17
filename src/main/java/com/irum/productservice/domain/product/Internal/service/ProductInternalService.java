package com.irum.productservice.domain.product.Internal.service;

import com.irum.global.advice.exception.CommonException;
import com.irum.openfeign.dto.response.ProductDto;
import com.irum.openfeign.product.dto.request.ProductInternalRequest;
import com.irum.openfeign.product.dto.request.RollbackStockRequest;
import com.irum.openfeign.product.dto.response.ProductInternalResponse;
import com.irum.productservice.domain.discount.domain.entity.Discount;
import com.irum.productservice.domain.discount.domain.repository.DiscountRepository;
import com.irum.productservice.domain.product.domain.entity.Product;
import com.irum.productservice.domain.product.domain.entity.ProductOptionValue;
import com.irum.productservice.domain.product.domain.repository.ProductOptionValueRepository;
import com.irum.productservice.domain.product.domain.repository.ProductRepository;
import com.irum.productservice.global.exception.errorcode.ProductErrorCode;
import jakarta.persistence.OptimisticLockException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.StaleObjectStateException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductInternalService {

    private final ProductRepository productRepository;
    private final ProductOptionValueRepository productOptionValueRepository;
    private final DiscountRepository discountRepository;
    private final ProductStockService productStockService;

    // 상품 ID를 가지고 상품, 옵션(전체), 할인 조회
    @Transactional(readOnly = true)
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
    @Transactional(readOnly = true)
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

    /** storeId, optionValueIdList -> 재고 감소 및 배송 정책, 상품 정보 조회 */
    @Retryable( // TODO : 낙관적 락 예외처리에 대한 재시도 횟수, 간격 : 정책 설정 필요
            retryFor = {
                OptimisticLockException.class,
                StaleObjectStateException.class,
                ObjectOptimisticLockingFailureException.class
            },
            noRetryFor = {CommonException.class},
            notRecoverable = {CommonException.class},
            maxAttempts = 3, // 최대 3번 재시도
            backoff = @Backoff(delay = 50, maxDelay = 500, multiplier = 1.5, random = true),
            recover = "recoverUpdateStock")
    public ProductInternalResponse updateStock(ProductInternalRequest request) {
        return productStockService.updateStockInTransaction(request);
    }

    /** updateStock 낙관적 락 충돌 재시도 횟수 초과시 처리 */
    @Recover
    public ProductInternalResponse recoverUpdateStock(Throwable e, ProductInternalRequest request) {
        log.error(
                "재고 차감 최종 실패, 3번의 재시도 모두 실패. 발생한 예외 {},  Request : {}",
                e.getClass().getSimpleName(),
                request);
        throw new CommonException(ProductErrorCode.PRODUCT_RETRY_LIMIT_EXCEEDED);
    }

    /** 주문에 포함된 모든 상품의 재고를 다시 늘립니다. */
    @Retryable( // TODO : 낙관적 락 예외처리에 대한 재시도 횟수, 간격 : 정책 설정 필요
            retryFor = {
                OptimisticLockException.class,
                StaleObjectStateException.class,
                ObjectOptimisticLockingFailureException.class
            },
            noRetryFor = {CommonException.class},
            notRecoverable = {CommonException.class},
            maxAttempts = 10,
            backoff = @Backoff(delay = 100, maxDelay = 1000, multiplier = 1.5, random = true),
            recover = "recoverRollbackStock")
    public void rollbackStock(RollbackStockRequest request) {
        productStockService.rollbackStockInTransactional(request);
    }

    @Recover
    public void recoverRollbackStock(Throwable e, RollbackStockRequest request) {
        log.error(
                "재고 롤백 최종 실패, 설정한 재시도 모두 실패. 발생한 예외 {}, Request : {}",
                e.getClass().getSimpleName(),
                request);
        throw new CommonException(ProductErrorCode.PRODUCT_RETRY_LIMIT_EXCEEDED);
    }
}
