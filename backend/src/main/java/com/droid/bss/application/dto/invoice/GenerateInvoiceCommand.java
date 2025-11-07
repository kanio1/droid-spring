package com.droid.bss.application.dto.invoice;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Stub class for GenerateInvoiceCommand
 * Minimal implementation for testing purposes
 */
@Schema(name = "GenerateInvoiceCommand", description = "Command to generate invoice")
public record GenerateInvoiceCommand(
    @Schema(description = "Order ID", example = "123e4567-e89b-12d3-a456-426614174000")
    @NotNull
    String orderId,

    @Schema(description = "Customer ID", example = "123e4567-e89b-12d3-a456-426614174000")
    @NotNull
    String customerId,

    @Schema(description = "Invoice date", example = "2024-01-01")
    @NotNull
    LocalDate invoiceDate,

    @Schema(description = "Due date", example = "2024-01-31")
    @NotNull
    LocalDate dueDate
) {
}
