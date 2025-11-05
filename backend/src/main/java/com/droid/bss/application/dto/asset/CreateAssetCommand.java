package com.droid.bss.application.dto.asset;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * Command for creating a new asset
 */
public record CreateAssetCommand(
        @NotNull String assetTag,
        @NotNull String assetType,
        @NotNull String name,
        String description,
        String serialNumber,
        String modelNumber,
        String manufacturer,
        LocalDate purchaseDate,
        LocalDate warrantyExpiry,
        String location,
        String costCenter,
        String notes
) {
}
