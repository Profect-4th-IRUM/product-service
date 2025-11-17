package com.irum.productservice.domain.product.event;

import jakarta.persistence.Lob;
import java.util.UUID;

public record AiDescriptionGeneratedEvent(
        UUID productId, @Lob String generatedDescription, boolean isSuccess) {}
