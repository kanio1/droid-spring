package com.droid.bss.application.dto.invoice;

import com.droid.bss.domain.invoice.InvoiceType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Command for updating an existing invoice
 */
@Schema(name = "UpdateInvoiceRequest", description = "Request to update an existing invoice")
public record UpdateInvoiceCommand(
    @Schema(
        description = "Unique invoice identifier",
        example = "123e4567-e89b-12d3-a456-426614174000",
        type = "string",
        format = "uuid"
    )
    @NotNull(message = "Invoice ID is required")
    UUID id,

    @Schema(
        description = "Unique invoice number",
        example = "INV-2024-000001",
        minLength = 3,
        maxLength = 50
    )
    @NotBlank(message = "Invoice number is required")
    @Size(min = 3, max = 50, message = "Invoice number must be between 3 and 50 characters")
    String invoiceNumber,

    @Schema(
        description = "Customer ID",
        example = "123e4567-e89b-12d3-a456-426614174000",
        type = "string",
        format = "uuid"
    )
    @NotNull(message = "Customer ID is required")
    String customerId,

    @Schema(
        description = "Invoice type",
        example = "RECURRING",
        implementation = String.class
    )
    @NotNull(message = "Invoice type is required")
    InvoiceType invoiceType,

    @Schema(
        description = "Invoice issue date",
        example = "2024-01-01",
        type = "string",
        format = "date"
    )
    @NotNull(message = "Issue date is required")
    LocalDate issueDate,

    @Schema(
        description = "Invoice due date",
        example = "2024-01-31",
        type = "string",
        format = "date"
    )
    @NotNull(message = "Due date is required")
    LocalDate dueDate,

    @Schema(
        description = "Paid date",
        example = "2024-01-25",
        type = "string",
        format = "date"
    )
    LocalDate paidDate,

    @Schema(
        description = "Subtotal amount (before tax and discounts)",
        example = "300.00",
        minimum = "0"
    )
    @NotNull(message = "Subtotal is required")
    @DecimalMin(value = "0.0", message = "Subtotal must be greater than or equal to 0")
    BigDecimal subtotal,

    @Schema(
        description = "Discount amount",
        example = "0.00",
        minimum = "0"
    )
    BigDecimal discountAmount,

    @Schema(
        description = "Tax amount",
        example = "69.00",
        minimum = "0"
    )
    @NotNull(message = "Tax amount is required")
    @DecimalMin(value = "0.0", message = "Tax amount must be greater than or equal to 0")
    BigDecimal taxAmount,

    @Schema(
        description = "Total amount",
        example = "369.00",
        minimum = "0"
    )
    @NotNull(message = "Total amount is required")
    @DecimalMin(value = "0.0", message = "Total amount must be greater than or equal to 0")
    BigDecimal totalAmount,

    @Schema(
        description = "Currency code (ISO 4217)",
        example = "PLN",
        maxLength = 3
    )
    @Size(max = 3, message = "Currency must be 3 characters")
    String currency,

    @Schema(
        description = "Payment terms in days",
        example = "30"
    )
    Integer paymentTerms,

    @Schema(
        description = "Late fee amount",
        example = "0.00",
        minimum = "0"
    )
    BigDecimal lateFee,

    @Schema(
        description = "Additional notes or comments",
        example = "Payment expected within 30 days",
        maxLength = 2000
    )
    @Size(max = 2000, message = "Notes cannot exceed 2000 characters")
    String notes,

    @Schema(
        description = "PDF URL (if invoice PDF is hosted)",
        example = "https://storage.example.com/invoices/INV-2024-000001.pdf"
    )
    @Size(max = 500, message = "PDF URL cannot exceed 500 characters")
    String pdfUrl,

    @Schema(
        description = "Billing period start date",
        example = "2024-01-01",
        type = "string",
        format = "date"
    )
    LocalDate billingPeriodStart,

    @Schema(
        description = "Billing period end date",
        example = "2024-01-31",
        type = "string",
        format = "date"
    )
    LocalDate billingPeriodEnd,

    @Schema(
        description = "Version number for optimistic locking",
        example = "1"
    )
    @NotNull(message = "Version is required for optimistic locking")
    Long version
) {
}
