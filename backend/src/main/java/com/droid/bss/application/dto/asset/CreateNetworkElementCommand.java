package com.droid.bss.application.dto.asset;

import jakarta.validation.constraints.NotNull;

/**
 * Command for creating a new network element
 */
public record CreateNetworkElementCommand(
        @NotNull String elementId,
        @NotNull String elementType,
        @NotNull String name,
        String description,
        String ipAddress,
        String macAddress,
        String firmwareVersion,
        String softwareVersion,
        String location,
        String rackPosition,
        Integer portCount,
        String capacity,
        String status
) {
}
