package com.droid.bss.application.dto.order;

import com.droid.bss.domain.order.OrderType;
import com.droid.bss.domain.order.OrderStatus;
import com.droid.bss.domain.order.OrderPriority;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Command for creating a new order
 */
@Schema(name = "CreateOrderRequest", description = "Request to create a new order")
public record CreateOrderCommand(
    @Schema(
        description = "Unique order number",
        example = "ORD-2024-000001",
        minLength = 3,
        maxLength = 50
    )
    @NotBlank(message = "Order number is required")
    @Size(min = 3, max = 50, message = "Order number must be between 3 and 50 characters")
    String orderNumber,

    @Schema(
        description = "Customer ID who placed the order",
        example = "123e4567-e89b-12d3-a456-426614174000",
        type = "string",
        format = "uuid"
    )
    @NotNull(message = "Customer ID is required")
    String customerId,

    @Schema(
        description = "Order type",
        example = "NEW",
        implementation = String.class
    )
    @NotNull(message = "Order type is required")
    OrderType orderType,

    @Schema(
        description = "Initial order status",
        example = "PENDING",
        implementation = String.class
    )
    @NotNull(message = "Order status is required")
    OrderStatus status,

    @Schema(
        description = "Order priority",
        example = "NORMAL",
        implementation = String.class
    )
    @NotNull(message = "Order priority is required")
    OrderPriority priority,

    @Schema(
        description = "Order total amount",
        example = "299.99",
        minimum = "0"
    )
    @NotNull(message = "Total amount is required")
    java.math.BigDecimal totalAmount,

    @Schema(
        description = "Currency code (ISO 4217)",
        example = "PLN",
        maxLength = 3
    )
    @Size(max = 3, message = "Currency must be 3 characters")
    String currency,

    @Schema(
        description = "Requested delivery/provisioning date",
        example = "2024-01-15",
        type = "string",
        format = "date"
    )
    LocalDate requestedDate,

    @Schema(
        description = "Sales channel where order was placed",
        example = "WEB",
        maxLength = 50
    )
    @Size(max = 50, message = "Sales channel cannot exceed 50 characters")
    String orderChannel,

    @Schema(
        description = "Sales representative ID",
        example = "SALES001",
        maxLength = 100
    )
    @Size(max = 100, message = "Sales rep ID cannot exceed 100 characters")
    String salesRepId,

    @Schema(
        description = "Additional notes or comments",
        example = "Customer requested priority installation",
        maxLength = 2000
    )
    @Size(max = 2000, message = "Notes cannot exceed 2000 characters")
    String notes
) {
}
