package com.irum.productservice.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.irum.openfeign.product.dto.request.UpdateStockRequest;
import com.irum.openfeign.product.dto.response.UpdateStockDto;
import com.irum.productservice.domain.product.Internal.service.ProductInternalService;
import com.irum.productservice.domain.product.Internal.service.ProductStockService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductInternalServiceTest {

    @InjectMocks private ProductInternalService productInternalService;

    @Mock private ProductStockService productStockService;

    @Test
    @DisplayName("재고 업데이트 성공 - ProductStockService로 위임")
    void updateStock_Success() {
        // given
        UUID storeId = UUID.randomUUID();
        UUID optionValueId1 = UUID.randomUUID();
        UUID optionValueId2 = UUID.randomUUID();

        UpdateStockRequest.OptionValueRequest reqOption1 =
                new UpdateStockRequest.OptionValueRequest(optionValueId1, 5);
        UpdateStockRequest.OptionValueRequest reqOption2 =
                new UpdateStockRequest.OptionValueRequest(optionValueId2, 10);

        UpdateStockRequest request =
                new UpdateStockRequest(List.of(reqOption1, reqOption2), storeId);

        // ProductStockService가 반환할 DTO
        UpdateStockDto expectedDto = org.mockito.Mockito.mock(UpdateStockDto.class);

        given(productStockService.updateStockInTransaction(any(UpdateStockRequest.class)))
                .willReturn(expectedDto);

        // when
        UpdateStockDto result = productInternalService.updateStock(request);

        // then
        assertThat(result).isSameAs(expectedDto);
        verify(productStockService).updateStockInTransaction(request);
        verifyNoMoreInteractions(productStockService);
    }
}
