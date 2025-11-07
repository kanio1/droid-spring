package com.droid.bss.application.dto.invoice;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Stub class for SendInvoiceCommand
 * Minimal implementation for testing purposes
 */
@Schema(name = "SendInvoiceCommand", description = "Command to send invoice")
public record SendInvoiceCommand(
    @Schema(description = "Invoice ID", example = "123e4567-e89b-12d3-a456-426614174000")
    @NotNull
    String invoiceId,

    @Schema(description = "Recipient email", example = "customer@example.com")
    @NotNull
    String email,

    @Schema(description = "Additional message")
    String message,

    @Schema(description = "When to send")
    LocalDateTime scheduledAt
) {
}
