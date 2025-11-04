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
 * Command for updating an existing payment
 */
@Schema(name = "UpdatePaymentRequest", description = "Request to update an existing payment")
public record UpdatePaymentCommand(
    @NotBlank(message = "ID is required")
    String id,

    BigDecimal amount,

    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be 3-letter ISO code (e.g., PLN, EUR, USD)")
    String currency,

    PaymentMethod paymentMethod,

    LocalDate paymentDate,

    LocalDate receivedDate,

    String referenceNumber,

    String notes
) {

}
