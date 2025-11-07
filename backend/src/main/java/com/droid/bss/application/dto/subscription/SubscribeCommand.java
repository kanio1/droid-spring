package com.droid.bss.application.dto.subscription;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Stub class for SubscribeCommand
 * Minimal implementation for testing purposes
 */
@Schema(name = "SubscribeCommand", description = "Command to subscribe to a product")
public record SubscribeCommand(

    @Schema(description = "Customer ID", example = "123e4567-e89b-12d3-a456-426614174000")
    @NotNull
    String customerId,

    @Schema(description = "Product ID", example = "123e4567-e89b-12d3-a456-426614174000")
    @NotNull
    String productId,

    @Schema(description = "Subscription plan", example = "MONTHLY")
    String plan,

    @Schema(description = "Start date")
    LocalDate startDate,

    @Schema(description = "End date")
    LocalDate endDate,

    @Schema(description = "Auto-renewal enabled")
    Boolean autoRenew,

    @Schema(description = "Subscription metadata")
    List<SubscriptionMetadata> metadata
) {

    /**
     * Stub class for SubscriptionMetadata
     * Minimal implementation for testing purposes
     */
    public record SubscriptionMetadata(
        String key,
        String value
    ) {
    }
}
