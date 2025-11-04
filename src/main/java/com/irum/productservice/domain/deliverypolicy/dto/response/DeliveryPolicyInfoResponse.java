package com.irum.productservice.domain.deliverypolicy.dto.response;

import com.irum.productservice.domain.deliverypolicy.domain.entity.DeliveryPolicy;
import java.util.UUID;

public record DeliveryPolicyInfoResponse(
        UUID id, int defaultDeliveryFee, int minQuantity, int minAmount) {
    public static DeliveryPolicyInfoResponse from(DeliveryPolicy deliveryPolicy) {
        return new DeliveryPolicyInfoResponse(
                deliveryPolicy.getId(),
                deliveryPolicy.getDefaultDeliveryFee(),
                deliveryPolicy.getMinAmount(),
                deliveryPolicy.getMinQuantity());
    }
}
