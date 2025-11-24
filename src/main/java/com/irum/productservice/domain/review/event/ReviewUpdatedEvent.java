package com.irum.productservice.domain.review.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@Getter
@RequiredArgsConstructor
public class ReviewUpdatedEvent {
    private final UUID productId;   // 어떤 상품의 리뷰가 바뀌었는지
}