package com.droid.bss.application.dto.asset;

import jakarta.validation.constraints.NotNull;

/**
 * Command for assigning an asset
 */
public record AssignAssetCommand(
        @NotNull String assignedToType,
        @NotNull String assignedToId,
        @NotNull String assignedToName
) {
}
