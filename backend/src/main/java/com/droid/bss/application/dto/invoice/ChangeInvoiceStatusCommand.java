package com.droid.bss.application.dto.invoice;

import com.droid.bss.domain.invoice.InvoiceStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Command for changing invoice status
 */
@Schema(name = "ChangeInvoiceStatusRequest", description = "Request to change invoice status")
public record ChangeInvoiceStatusCommand(
    @Schema(
        description = "Unique invoice identifier",
        example = "123e4567-e89b-12d3-a456-426614174000",
        type = "string",
        format = "uuid"
    )
    @NotNull(message = "Invoice ID is required")
    UUID id,

    @Schema(
        description = "New invoice status",
        example = "SENT",
        implementation = String.class
    )
    @NotNull(message = "Invoice status is required")
    InvoiceStatus status,

    @Schema(
        description = "Paid date (required when status is PAID)",
        example = "2024-01-25",
        type = "string",
        format = "date"
    )
    LocalDate paidDate,

    @Schema(
        description = "Email address where invoice was sent (required when status is SENT)",
        example = "customer@example.com"
    )
    String sentToEmail,

    @Schema(
        description = "Timestamp when invoice was sent",
        example = "2024-01-01T10:00:00",
        type = "string",
        format = "date-time"
    )
    java.time.LocalDateTime sentAt,

    @Schema(
        description = "Version number for optimistic locking",
        example = "1"
    )
    @NotNull(message = "Version is required for optimistic locking")
    Long version
) {
}
