package com.irum.productservice.domain.store.event;


import com.irum.productservice.domain.deliverypolicy.service.DeliveryPolicyService;
import com.irum.productservice.domain.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StoreEventListener {

    private final ProductService productService;
    private final DeliveryPolicyService deliveryPolicyService;

    @EventListener
    public void handleStoreDeleted(StoreDeletedEvent event) {
        productService.deleteProductsByStoreId(event.getStoreId(),event.getDeletedBy());
        deliveryPolicyService.deleteDeliveryPolicyByStoreId(event.getStoreId(),event.getDeletedBy());
    }

}
