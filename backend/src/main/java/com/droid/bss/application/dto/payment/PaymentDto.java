package com.droid.bss.application.dto.payment;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Stub class for PaymentDto
 * Minimal implementation for testing purposes
 */
@Schema(name = "PaymentDto", description = "Payment data transfer object")
public record PaymentDto(

    @Schema(description = "Payment ID")
    String id,

    @Schema(description = "Payment number", example = "PAY-2024-0001")
    String paymentNumber,

    @Schema(description = "Invoice ID")
    String invoiceId,

    @Schema(description = "Customer ID")
    String customerId,

    @Schema(description = "Payment amount")
    BigDecimal amount,

    @Schema(description = "Currency code", example = "PLN")
    String currency,

    @Schema(description = "Payment method")
    PaymentMethod paymentMethod,

    @Schema(description = "Payment status", example = "COMPLETED")
    String status,

    @Schema(description = "Transaction ID")
    String transactionId,

    @Schema(description = "Creation timestamp")
    LocalDateTime createdAt,

    @Schema(description = "Last update timestamp")
    LocalDateTime updatedAt
) {
}
