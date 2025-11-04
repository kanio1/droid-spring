package com.droid.bss.application.dto.invoice;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for Invoice entity
 */
@Schema(name = "InvoiceResponse", description = "Invoice response with full details")
public record InvoiceResponse(
    @Schema(
        description = "Unique invoice identifier",
        example = "123e4567-e89b-12d3-a456-426614174000",
        type = "string",
        format = "uuid"
    )
    UUID id,

    @Schema(
        description = "Unique invoice number",
        example = "INV-2024-000001"
    )
    String invoiceNumber,

    @Schema(
        description = "Customer ID",
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
        description = "Invoice type code",
        example = "RECURRING"
    )
    String invoiceType,

    @Schema(
        description = "Invoice type display name",
        example = "Cykliczna"
    )
    String invoiceTypeDisplayName,

    @Schema(
        description = "Invoice status code",
        example = "SENT"
    )
    String status,

    @Schema(
        description = "Invoice status display name",
        example = "Wysłana"
    )
    String statusDisplayName,

    @Schema(
        description = "Invoice issue date",
        example = "2024-01-01",
        type = "string",
        format = "date"
    )
    LocalDate issueDate,

    @Schema(
        description = "Invoice due date",
        example = "2024-01-31",
        type = "string",
        format = "date"
    )
    LocalDate dueDate,

    @Schema(
        description = "Paid date",
        example = "2024-01-25",
        type = "string",
        format = "date"
    )
    LocalDate paidDate,

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
        description = "Subtotal amount (before tax and discounts)",
        example = "300.00"
    )
    BigDecimal subtotal,

    @Schema(
        description = "Discount amount",
        example = "0.00"
    )
    BigDecimal discountAmount,

    @Schema(
        description = "Tax amount",
        example = "69.00"
    )
    BigDecimal taxAmount,

    @Schema(
        description = "Total amount",
        example = "369.00"
    )
    BigDecimal totalAmount,

    @Schema(
        description = "Currency code",
        example = "PLN"
    )
    String currency,

    @Schema(
        description = "Payment terms in days",
        example = "30"
    )
    Integer paymentTerms,

    @Schema(
        description = "Late fee amount",
        example = "0.00"
    )
    BigDecimal lateFee,

    @Schema(
        description = "Additional notes or comments",
        example = "Payment expected within 30 days"
    )
    String notes,

    @Schema(
        description = "PDF URL (if invoice PDF is hosted)",
        example = "https://storage.example.com/invoices/INV-2024-000001.pdf"
    )
    String pdfUrl,

    @Schema(
        description = "Email address where invoice was sent",
        example = "customer@example.com"
    )
    String sentToEmail,

    @Schema(
        description = "Timestamp when invoice was sent",
        example = "2024-01-01T10:00:00",
        type = "string",
        format = "date-time"
    )
    LocalDateTime sentAt,

    @Schema(
        description = "Whether the invoice is unpaid",
        example = "true"
    )
    boolean isUnpaid,

    @Schema(
        description = "Whether the invoice is overdue",
        example = "false"
    )
    boolean isOverdue,

    @Schema(
        description = "Whether the invoice is paid",
        example = "false"
    )
    boolean isPaid,

    @Schema(
        description = "Whether the invoice can be cancelled",
        example = "true"
    )
    boolean canBeCancelled,

    @Schema(
        description = "Number of items in the invoice",
        example = "5"
    )
    int itemCount,

    @Schema(
        description = "Timestamp when the invoice was created",
        example = "2024-01-01T10:00:00",
        type = "string",
        format = "date-time"
    )
    LocalDateTime createdAt,

    @Schema(
        description = "Timestamp when the invoice was last updated",
        example = "2024-01-15T14:30:00",
        type = "string",
        format = "date-time"
    )
    LocalDateTime updatedAt,

    @Schema(
        description = "User who created the invoice",
        example = "admin@company.com"
    )
    String createdBy,

    @Schema(
        description = "User who last updated the invoice",
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
     * Convert InvoiceEntity to InvoiceResponse
     */
    public static InvoiceResponse from(com.droid.bss.domain.invoice.InvoiceEntity invoice) {
        return new InvoiceResponse(
            invoice.getId(),
            invoice.getInvoiceNumber(),
            invoice.getCustomer() != null ? invoice.getCustomer().getId().toString() : null,
            invoice.getCustomer() != null ?
                invoice.getCustomer().getFirstName() + " " + invoice.getCustomer().getLastName() : null,
            invoice.getInvoiceType() != null ? invoice.getInvoiceType().name() : null,
            invoice.getInvoiceType() != null ? invoice.getInvoiceType().name() : null, // Display name
            invoice.getStatus() != null ? invoice.getStatus().name() : null,
            invoice.getStatus() != null ? invoice.getStatus().name() : null, // Display name
            invoice.getIssueDate(),
            invoice.getDueDate(),
            invoice.getPaidDate(),
            invoice.getBillingPeriodStart(),
            invoice.getBillingPeriodEnd(),
            invoice.getSubtotal(),
            invoice.getDiscountAmount(),
            invoice.getTaxAmount(),
            invoice.getTotalAmount(),
            invoice.getCurrency(),
            invoice.getPaymentTerms(),
            invoice.getLateFee(),
            invoice.getNotes(),
            invoice.getPdfUrl(),
            invoice.getSentToEmail(),
            invoice.getSentAt(),
            invoice.isDraft() || invoice.getStatus().name().equals("ISSUED") || invoice.getStatus().name().equals("SENT") || invoice.getStatus().name().equals("OVERDUE"),
            invoice.isOverdue(),
            invoice.isPaid(),
            invoice.canBeCancelled(),
            invoice.getItems() != null ? invoice.getItems().size() : 0,
            invoice.getCreatedAt(),
            invoice.getUpdatedAt(),
            invoice.getCreatedBy(),
            invoice.getUpdatedBy(),
            invoice.getVersion() != null ? invoice.getVersion().longValue() : null
        );
    }
}
