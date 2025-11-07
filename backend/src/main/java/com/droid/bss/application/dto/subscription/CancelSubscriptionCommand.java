package com.droid.bss.application.dto.subscription;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * Stub class for CancelSubscriptionCommand
 * Minimal implementation for testing purposes
 */
@Schema(name = "CancelSubscriptionCommand", description = "Command to cancel a subscription")
public record CancelSubscriptionCommand(

    @Schema(description = "Subscription ID", example = "123e4567-e89b-12d3-a456-426614174000")
    @NotNull
    String subscriptionId,

    @Schema(description = "Reason for cancellation")
    String reason,

    @Schema(description = "When the subscription was cancelled")
    LocalDateTime cancelledAt
) {
}
