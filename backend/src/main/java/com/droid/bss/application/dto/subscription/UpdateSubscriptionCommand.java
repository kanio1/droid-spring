package com.droid.bss.application.dto.subscription;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Command for updating an existing subscription
 */
@Schema(name = "UpdateSubscriptionRequest", description = "Request to update an existing subscription")
public record UpdateSubscriptionCommand(
    String id,

    LocalDate endDate,

    LocalDate nextBillingDate,

    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    BigDecimal price,

    String currency,

    String billingPeriod,

    boolean autoRenew
) {

}
