package com.droid.bss.application.dto.payment;

import com.droid.bss.domain.payment.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Command for changing payment status
 */
@Schema(name = "ChangePaymentStatusRequest", description = "Request to change payment status")
public record ChangePaymentStatusCommand(
    @NotBlank(message = "ID is required")
    String id,

    @NotNull(message = "Status is required")
    PaymentStatus status,

    String reason
) {

}
