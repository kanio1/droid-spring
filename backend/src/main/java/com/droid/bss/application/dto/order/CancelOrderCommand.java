package com.droid.bss.application.dto.order;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * Stub class for CancelOrderCommand
 * Minimal implementation for testing purposes
 */
@Schema(name = "CancelOrderCommand", description = "Command to cancel an order")
public record CancelOrderCommand(

    @Schema(description = "Order ID", example = "123e4567-e89b-12d3-a456-426614174000")
    @NotNull
    String orderId,

    @Schema(description = "Reason for cancellation")
    String reason,

    @Schema(description = "When the order was cancelled")
    LocalDateTime cancelledAt
) {
}
