package com.droid.bss.application.dto.asset;

import com.droid.bss.domain.asset.AssetEntity;
import com.droid.bss.domain.asset.AssetStatus;
import com.droid.bss.domain.asset.AssetType;

import java.time.LocalDate;

/**
 * Response DTO for asset data
 */
public record AssetResponse(
        String id,
        String assetTag,
        String assetType,
        String name,
        String description,
        String serialNumber,
        String modelNumber,
        String manufacturer,
        String status,
        LocalDate purchaseDate,
        LocalDate warrantyExpiry,
        String location,
        String assignedToType,
        String assignedToId,
        String assignedToName,
        LocalDate assignedDate,
        String costCenter,
        String notes
) {

    public static AssetResponse from(AssetEntity asset) {
        return new AssetResponse(
                asset.getId(),
                asset.getAssetTag(),
                asset.getAssetType().name(),
                asset.getName(),
                asset.getDescription(),
                asset.getSerialNumber(),
                asset.getModelNumber(),
                asset.getManufacturer(),
                asset.getStatus().name(),
                asset.getPurchaseDate(),
                asset.getWarrantyExpiry(),
                asset.getLocation(),
                asset.getAssignedToType(),
                asset.getAssignedToId(),
                asset.getAssignedToName(),
                asset.getAssignedDate(),
                asset.getCostCenter(),
                asset.getNotes()
        );
    }
}
