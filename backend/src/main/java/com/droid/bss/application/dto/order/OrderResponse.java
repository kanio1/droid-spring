package com.droid.bss.application.dto.order;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for Order entity
 */
@Schema(name = "OrderResponse", description = "Order response with full details")
public record OrderResponse(
    @Schema(
        description = "Unique order identifier",
        example = "123e4567-e89b-12d3-a456-426614174000",
        type = "string",
        format = "uuid"
    )
    UUID id,

    @Schema(
        description = "Unique order number",
        example = "ORD-2024-000001"
    )
    String orderNumber,

    @Schema(
        description = "Customer ID who placed the order",
        example = "123e4567-e89b-12d3-a456-426614174000",
        type = "string",
        format = "uuid"
    )
    String customerId,

    @Schema(
        description = "Customer name",
        example = "John Doe"
    )
    String customerName,

    @Schema(
        description = "Order type code",
        example = "NEW"
    )
    String orderType,

    @Schema(
        description = "Order type display name",
        example = "Nowe zamówienie"
    )
    String orderTypeDisplayName,

    @Schema(
        description = "Order status code",
        example = "PENDING"
    )
    String status,

    @Schema(
        description = "Order status display name",
        example = "Oczekujące"
    )
    String statusDisplayName,

    @Schema(
        description = "Order priority code",
        example = "NORMAL"
    )
    String priority,

    @Schema(
        description = "Order priority display name",
        example = "Normalny"
    )
    String priorityDisplayName,

    @Schema(
        description = "Order total amount",
        example = "299.99"
    )
    BigDecimal totalAmount,

    @Schema(
        description = "Currency code",
        example = "PLN"
    )
    String currency,

    @Schema(
        description = "Requested delivery/provisioning date",
        example = "2024-01-15",
        type = "string",
        format = "date"
    )
    LocalDate requestedDate,

    @Schema(
        description = "Promised delivery/provisioning date",
        example = "2024-01-20",
        type = "string",
        format = "date"
    )
    LocalDate promisedDate,

    @Schema(
        description = "Actual completion date",
        example = "2024-01-18",
        type = "string",
        format = "date"
    )
    LocalDate completedDate,

    @Schema(
        description = "Sales channel where order was placed",
        example = "WEB"
    )
    String orderChannel,

    @Schema(
        description = "Sales representative ID",
        example = "SALES001"
    )
    String salesRepId,

    @Schema(
        description = "Sales representative name",
        example = "Jane Smith"
    )
    String salesRepName,

    @Schema(
        description = "Additional notes or comments",
        example = "Customer requested priority installation"
    )
    String notes,

    @Schema(
        description = "Whether the order is pending",
        example = "true"
    )
    boolean isPending,

    @Schema(
        description = "Whether the order is completed",
        example = "false"
    )
    boolean isCompleted,

    @Schema(
        description = "Whether the order can be cancelled",
        example = "true"
    )
    boolean canBeCancelled,

    @Schema(
        description = "Number of items in the order",
        example = "3"
    )
    int itemCount,

    @Schema(
        description = "Timestamp when the order was created",
        example = "2024-01-01T10:00:00",
        type = "string",
        format = "date-time"
    )
    LocalDateTime createdAt,

    @Schema(
        description = "Timestamp when the order was last updated",
        example = "2024-01-15T14:30:00",
        type = "string",
        format = "date-time"
    )
    LocalDateTime updatedAt,

    @Schema(
        description = "User who created the order",
        example = "admin@company.com"
    )
    String createdBy,

    @Schema(
        description = "User who last updated the order",
        example = "admin@company.com"
    )
    String updatedBy,

    @Schema(
        description = "Version number for optimistic locking",
        example = "1"
    )
    Long version
) {

    /**
     * Convert OrderEntity to OrderResponse
     */
    // public static OrderResponse from(OrderEntity order) {
    //     return new OrderResponse(
    //         order.getId(),
    //         order.getOrderNumber(),
    //         order.getCustomer().getId().toString(),
    //         order.getCustomer().getFirstName() + " " + order.getCustomer().getLastName(),
    //         order.getOrderType().name(),
    //         order.getOrderType().getDisplayName(),
    //         order.getStatus().name(),
    //         order.getStatus().getDisplayName(),
    //         order.getPriority().name(),
    //         order.getPriority().getDisplayName(),
    //         order.getTotalAmount(),
    //         order.getCurrency(),
    //         order.getRequestedDate(),
    //         order.getPromisedDate(),
    //         order.getCompletedDate(),
    //         order.getOrderChannel(),
    //         order.getSalesRepId(),
    //         null, // Would need to look up sales rep name
    //         order.getNotes(),
    //         order.isPending(),
    //         order.isCompleted(),
    //         order.canBeCancelled(),
    //         order.getItems().size(),
    //         order.getCreatedAt(),
    //         order.getUpdatedAt(),
    //         order.getCreatedBy(),
    //         order.getUpdatedBy(),
    //         order.getVersion()
    //     );
    // }
}
