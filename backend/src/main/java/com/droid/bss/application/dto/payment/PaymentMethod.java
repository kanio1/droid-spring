package com.droid.bss.application.dto.payment;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Stub enum for PaymentMethod
 * Minimal implementation for testing purposes
 */
@Schema(name = "PaymentMethod", description = "Payment method types")
public enum PaymentMethod {
    @Schema(description = "Credit card payment")
    CREDIT_CARD,

    @Schema(description = "Bank transfer payment")
    BANK_TRANSFER,

    @Schema(description = "PayPal payment")
    PAYPAL,

    @Schema(description = "Cash payment")
    CASH,

    @Schema(description = "Cryptocurrency payment")
    CRYPTOCURRENCY
}
