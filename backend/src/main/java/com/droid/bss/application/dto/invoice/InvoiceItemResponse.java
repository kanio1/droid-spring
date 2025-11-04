package com.droid.bss.application.dto.invoice;

import com.droid.bss.domain.invoice.InvoiceItemType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Response DTO for InvoiceItem entity
 */
@Schema(name = "InvoiceItemResponse", description = "Invoice item response with details")
public record InvoiceItemResponse(
    @Schema(
        description = "Unique invoice item identifier",
        example = "123e4567-e89b-12d3-a456-426614174000",
        type = "string",
        format = "uuid"
    )
    UUID id,

    @Schema(
        description = "Invoice ID",
        example = "123e4567-e89b-12d3-a456-426614174000",
        type = "string",
        format = "uuid"
    )
    UUID invoiceId,

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
        description = "Item description",
        example = "Premium Mobile Plan - January 2024"
    )
    String description,

    @Schema(
        description = "Quantity",
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
        description = "Sort order for display",
        example = "1"
    )
    int sortOrder,

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
    String statusDisplayName
) {
}
