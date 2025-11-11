package com.irum.openfeign.dto.response;

import java.util.UUID;

public record OrderDetailDto(
        UUID orderDetailId, UUID productId, Long memberId, String orderStatus) {}
