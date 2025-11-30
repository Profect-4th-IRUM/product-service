package com.irum.productservice.domain.product.consumer;

import com.irum.productservice.domain.product.Internal.service.ProductInternalService;
import com.irum.productservice.domain.product.event.OrderFailedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductEventConsumer {
    private final ProductInternalService productInternalService;

    @KafkaListener(
            topics= "${spring.kafka.topics.order-failed}",
            groupId="${spring.kafka.consumer.group-id}"
    )
    public void handleOrderPrepared( OrderFailedEvent event) {
        log.info("[외부] Order Failed event 수신 완료 {}", event);

        productInternalService.rollbackStock(event);
    }
}
