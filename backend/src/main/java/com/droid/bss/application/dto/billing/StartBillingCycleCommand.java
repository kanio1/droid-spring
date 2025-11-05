package com.droid.bss.application.dto.billing;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * Command to start a billing cycle
 */
public record StartBillingCycleCommand(
        @NotNull
        String customerId,

        @NotNull
        LocalDate cycleStart,

        @NotNull
        LocalDate cycleEnd,

        @NotNull
        LocalDate billingDate,

        @NotNull
        String cycleType
) {
}
