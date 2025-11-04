package com.droid.bss.application.dto.order;

import com.droid.bss.domain.order.OrderItemType;
import com.droid.bss.domain.order.OrderItemStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Response DTO for OrderItem entity
 */
@Schema(name = "OrderItemResponse", description = "Order item response with details")
public record OrderItemResponse(
    @Schema(
        description = "Unique order item identifier",
        example = "123e4567-e89b-12d3-a456-426614174000",
        type = "string",
        format = "uuid"
    )
    UUID id,

    @Schema(
        description = "Order ID",
        example = "123e4567-e89b-12d3-a456-426614174000",
        type = "string",
        format = "uuid"
    )
    UUID orderId,

    @Schema(
        description = "Product ID (if applicable)",
        example = "123e4567-e89b-12d3-a456-426614174000",
        type = "string",
        format = "uuid"
    )
    UUID productId,

    @Schema(
        description = "Product code (if applicable)",
        example = "PROD-001"
    )
    String productCode,

    @Schema(
        description = "Item type",
        example = "PRODUCT",
        implementation = String.class
    )
    String itemType,

    @Schema(
        description = "Item type display name",
        example = "Produkt"
    )
    String itemTypeDisplayName,

    @Schema(
        description = "Item code or identifier",
        example = "PROD-001"
    )
    String itemCode,

    @Schema(
        description = "Item name or description",
        example = "Premium Mobile Plan"
    )
    String itemName,

    @Schema(
        description = "Item description",
        example = "Unlimited calls, SMS, and 50GB data per month"
    )
    String itemDescription,

    @Schema(
        description = "Quantity ordered",
        example = "1",
        minimum = "0"
    )
    int quantity,

    @Schema(
        description = "Unit price",
        example = "99.99"
    )
    BigDecimal unitPrice,

    @Schema(
        description = "Total price (quantity * unit price)",
        example = "99.99"
    )
    BigDecimal totalPrice,

    @Schema(
        description = "Net amount before tax",
        example = "81.30"
    )
    BigDecimal netAmount,

    @Schema(
        description = "Tax amount",
        example = "18.69"
    )
    BigDecimal taxAmount,

    @Schema(
        description = "Tax rate (percentage)",
        example = "23.0"
    )
    BigDecimal taxRate,

    @Schema(
        description = "Discount amount",
        example = "0.00"
    )
    BigDecimal discountAmount,

    @Schema(
        description = "Item status",
        example = "ACTIVE",
        implementation = String.class
    )
    String status,

    @Schema(
        description = "Item status display name",
        example = "Aktywny"
    )
    String statusDisplayName,

    @Schema(
        description = "Whether the item is active",
        example = "true"
    )
    boolean isActive,

    @Schema(
        description = "Sort order for display",
        example = "1"
    )
    int sortOrder,

    @Schema(
        description = "Additional item properties as JSON",
        example = "{\"color\": \"black\", \"size\": \"large\"}"
    )
    String properties
) {
}
