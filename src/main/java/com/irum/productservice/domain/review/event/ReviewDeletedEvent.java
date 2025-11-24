package com.irum.productservice.domain.review.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@Getter
@RequiredArgsConstructor
public class ReviewDeletedEvent {
    private final UUID productId;
}
