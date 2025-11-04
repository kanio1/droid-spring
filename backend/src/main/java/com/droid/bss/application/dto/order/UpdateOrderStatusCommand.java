package com.droid.bss.application.dto.order;

import com.droid.bss.domain.order.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Command for updating order status
 */
@Schema(name = "UpdateOrderStatusRequest", description = "Request to update order status")
public record UpdateOrderStatusCommand(
    @Schema(
        description = "Unique order identifier",
        example = "123e4567-e89b-12d3-a456-426614174000",
        type = "string",
        format = "uuid"
    )
    @NotNull(message = "Order ID is required")
    UUID id,

    @Schema(
        description = "New order status",
        example = "IN_PROGRESS",
        implementation = String.class
    )
    @NotNull(message = "Order status is required")
    OrderStatus status,

    @Schema(
        description = "Version number for optimistic locking",
        example = "1"
    )
    @NotNull(message = "Version is required for optimistic locking")
    Long version
) {
}
