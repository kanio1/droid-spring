package com.droid.bss.application.dto.payment;

import com.droid.bss.domain.payment.PaymentMethod;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Command for creating a new payment
 */
@Schema(name = "CreatePaymentRequest", description = "Request to create a new payment")
public record CreatePaymentCommand(
    @NotBlank(message = "Customer ID is required")
    String customerId,

    String invoiceId,

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    BigDecimal amount,

    @NotBlank(message = "Currency is required")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be 3-letter ISO code (e.g., PLN, EUR, USD)")
    String currency,

    @NotNull(message = "Payment method is required")
    PaymentMethod paymentMethod,

    @NotNull(message = "Payment date is required")
    LocalDate paymentDate,

    String referenceNumber,
    String notes
) {

}
