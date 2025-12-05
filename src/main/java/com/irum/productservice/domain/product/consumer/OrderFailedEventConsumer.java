package com.irum.productservice.domain.product.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.irum.global.advice.exception.CommonException;
import com.irum.productservice.domain.product.Internal.service.ProductInternalService;
import com.irum.productservice.domain.product.event.OrderFailedEvent;
import com.irum.productservice.global.exception.errorcode.GlobalErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.DltStrategy;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderFailedEventConsumer {
    private final ProductInternalService productInternalService;
    private final ObjectMapper objectMapper;

    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 1000, multiplier = 2),
            dltStrategy = DltStrategy.FAIL_ON_ERROR,
            dltTopicSuffix = "-dlt")
    @KafkaListener(
            topics = "${spring.kafka.topics.order-failed}",
            groupId = "${spring.kafka.consumer.group-id}")
    public void handleOrderFailed(String message) {
        log.info("[외부] Order Failed event 수신 완료 {}", message);

        try {
            // String -> DTO 클래스 수동 변환
            OrderFailedEvent event = objectMapper.readValue(message, OrderFailedEvent.class);
            log.info("[변환] 객체 변환 성공");

            productInternalService.rollbackStock(event);
        } catch (JsonProcessingException e) {
            log.error("[변환] String -> DTO class 변환 실패 (JSON 형식이 안 맞음): message = {}, errorclass = {}, errormessage = {}", message, e.getClass(), e.getMessage());
            throw new CommonException(GlobalErrorCode.JSON_PROCESSING_EXCEPTION);
        }


    }

    @DltHandler // 재시도 기회를 모두 소진한 메시지가 DLQ에 도착하면, DLQ에서 메시지를 꺼내서 처리
    public void processDlt(
            @Payload String rawMessage,
            @Header(KafkaHeaders.ORIGINAL_TOPIC) String originalTopic,
            @Header(KafkaHeaders.ORIGINAL_OFFSET) long originalOffset,
            @Header(KafkaHeaders.ORIGINAL_PARTITION) int originalPartition,
            @Header(KafkaHeaders.ORIGINAL_CONSUMER_GROUP) String originalConsumerGroup,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.OFFSET) long offset,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.EXCEPTION_MESSAGE) String exception,
            @Header(KafkaHeaders.EXCEPTION_CAUSE_FQCN) String exceptionFQCN) {

        OrderFailedEvent event = null;
        try {

            event = objectMapper.readValue(rawMessage, OrderFailedEvent.class);
            log.error("DLT 파싱 성공");
        } catch (JsonProcessingException e) {
            log.error("[변환] String -> DTO class 변환 실패 (JSON 형식이 안 맞음): message = {}, errorclass = {}, errormessage = {}", rawMessage, e.getClass(), e.getMessage());
            throw new CommonException(GlobalErrorCode.JSON_PROCESSING_EXCEPTION);
        }

        log.error(
                "원본 토픽 : {} \t 원본 오프셋 : {} \t 원본 파티션 : {} \t 원본 컨슈머 그룹 : {}",
                originalTopic,
                originalOffset,
                originalPartition,
                originalConsumerGroup);
        log.error("DLT 토픽 : {} \t DLT 오프셋 : {} \t DLT 파티션 : {}", topic, offset, partition);
        log.error("예외 메시지 : {}", exception);
        log.error("예외 클래스 이름  : {}", exceptionFQCN);
        log.error("원본 메시지 : {}", event);
    }
}
