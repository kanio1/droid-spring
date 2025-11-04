package com.droid.bss.application.dto.subscription;

import com.droid.bss.domain.subscription.SubscriptionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Command for creating a new subscription
 */
@Schema(name = "CreateSubscriptionRequest", description = "Request to create a new subscription")
public record CreateSubscriptionCommand(
    @NotBlank(message = "Customer ID is required")
    String customerId,

    @NotBlank(message = "Product ID is required")
    String productId,

    String orderId,

    @NotNull(message = "Start date is required")
    LocalDate startDate,

    LocalDate endDate,

    @NotNull(message = "Billing start date is required")
    LocalDate billingStart,

    LocalDate nextBillingDate,

    @NotBlank(message = "Billing period is required")
    String billingPeriod,

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    BigDecimal price,

    String currency,

    boolean autoRenew
) {

}
