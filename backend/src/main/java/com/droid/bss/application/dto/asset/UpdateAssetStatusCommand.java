package com.droid.bss.application.dto.asset;

import jakarta.validation.constraints.NotNull;

/**
 * Command for updating asset status
 */
public record UpdateAssetStatusCommand(
        @NotNull String status,
        String notes
) {
}
