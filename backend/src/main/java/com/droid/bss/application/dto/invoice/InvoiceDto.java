package com.droid.bss.application.dto.invoice;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Stub class for InvoiceDto
 * Minimal implementation for testing purposes
 */
@Schema(name = "InvoiceDto", description = "Invoice data transfer object")
public record InvoiceDto(
    @Schema(description = "Invoice ID", example = "123e4567-e89b-12d3-a456-426614174000")
    String id,

    @Schema(description = "Invoice number", example = "INV-2024-000001")
    String invoiceNumber,

    @Schema(description = "Customer ID", example = "123e4567-e89b-12d3-a456-426614174000")
    String customerId,

    @Schema(description = "Invoice date", example = "2024-01-01")
    LocalDate invoiceDate,

    @Schema(description = "Due date", example = "2024-01-31")
    LocalDate dueDate,

    @Schema(description = "Total amount", example = "369.00")
    BigDecimal totalAmount,

    @Schema(description = "Currency", example = "PLN")
    String currency,

    @Schema(description = "Status", example = "PENDING")
    String status,

    @Schema(description = "Created at", example = "2024-01-01T10:00:00")
    LocalDateTime createdAt,

    @Schema(description = "Updated at", example = "2024-01-01T10:00:00")
    LocalDateTime updatedAt,

    @Schema(description = "Invoice items")
    List<InvoiceItemResponse> items
) {
}
