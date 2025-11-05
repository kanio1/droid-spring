package com.droid.bss.application.dto.asset;

import jakarta.validation.constraints.NotNull;

/**
 * Command for creating a new SIM card
 */
public record CreateSIMCardCommand(
        @NotNull String iccid,
        String msisdn,
        String imsi,
        String networkOperator,
        String apn,
        String status
) {
}
