package com.irum.productservice.domain.discount.dto.response;

import java.util.List;
import java.util.UUID;

public record DiscountInfoListResponse(
        List<DiscountInfoResponse> discountInfoList, UUID nextCursor, boolean hasNext) {}
