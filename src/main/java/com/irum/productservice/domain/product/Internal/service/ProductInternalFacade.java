package com.irum.productservice.domain.product.Internal.service;

import com.irum.global.advice.exception.CommonException;
import com.irum.openfeign.dto.request.UpdateStockRequest;
import com.irum.openfeign.dto.response.UpdateStockDto;
import com.irum.productservice.global.exception.errorcode.ProductErrorCode;
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.StaleObjectStateException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductInternalFacade {

    private final ProductInternalService productInternalService;

    // TODO : 낙관적 락 예외처리에 대한 재시도 횟수, 간격 : 정책 설정 필요
    @Retryable(
            retryFor = {OptimisticLockException.class, StaleObjectStateException.class, ObjectOptimisticLockingFailureException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 50, maxDelay = 500, multiplier = 1.5, random = true),
            recover = "recoverUpdateStock"
    )
    public UpdateStockDto updateStockWithRetry(UpdateStockRequest request) {
        return productInternalService.updateStock(request);
    }

    @Recover
    public UpdateStockDto recoverUpdateStock(Throwable e, UpdateStockRequest request) {
        log.error("재고 차감 최종 실패, 20번의 재시도 모두 실패. 발생한 예외 {},  Request : {}",  e.getClass().getSimpleName(), request);
        throw new CommonException(ProductErrorCode.PRODUCT_RETRY_LIMIT_EXCEEDED);
    }

}
