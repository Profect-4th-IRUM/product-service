package com.irum.productservice.domain.review.event;


import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@Getter
@RequiredArgsConstructor
public class ReviewCreatedEvent {
    private final UUID productId;
    private final int rate;   // 리뷰 평점
}