package com.droid.bss.application.dto.payment;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Stub class for ProcessPaymentCommand
 * Minimal implementation for testing purposes
 */
@Schema(name = "ProcessPaymentCommand", description = "Command to process a payment")
public record ProcessPaymentCommand(

    @Schema(description = "Invoice ID", example = "123e4567-e89b-12d3-a456-426614174000")
    @NotNull
    String invoiceId,

    @Schema(description = "Payment amount", example = "307.48")
    @NotNull
    BigDecimal amount,

    @Schema(description = "Payment method")
    @NotNull
    PaymentMethod paymentMethod,

    @Schema(description = "Card number or transaction ID", example = "CARD-1234-5678")
    String transactionId,

    @Schema(description = "Cardholder name", example = "John Doe")
    String cardholderName,

    @Schema(description = "CVV code", example = "123")
    String cvv,

    @Schema(description = "Expiry date", example = "12/26")
    String expiryDate
) {
}
