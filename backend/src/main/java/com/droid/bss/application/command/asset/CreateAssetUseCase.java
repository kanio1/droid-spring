package com.droid.bss.application.command.asset;

import com.droid.bss.application.dto.asset.AssetResponse;
import com.droid.bss.application.dto.asset.CreateAssetCommand;
import com.droid.bss.domain.asset.*;
import com.droid.bss.infrastructure.metrics.BusinessMetrics;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use case for creating assets
 */
@Service
public class CreateAssetUseCase {

    private final AssetRepository assetRepository;
    private final BusinessMetrics businessMetrics;

    public CreateAssetUseCase(AssetRepository assetRepository, BusinessMetrics businessMetrics) {
        this.assetRepository = assetRepository;
        this.businessMetrics = businessMetrics;
    }

    @Transactional
    public AssetResponse handle(CreateAssetCommand command) {
        // Check if asset tag already exists
        assetRepository.findByAssetTagAndDeletedAtIsNull(command.assetTag())
                .ifPresent(a -> {
                    throw new IllegalStateException("Asset tag already exists: " + command.assetTag());
                });

        // Create asset
        AssetEntity asset = new AssetEntity();
        asset.setAssetTag(command.assetTag());
        asset.setAssetType(AssetType.valueOf(command.assetType()));
        asset.setName(command.name());
        asset.setDescription(command.description());
        asset.setSerialNumber(command.serialNumber());
        asset.setModelNumber(command.modelNumber());
        asset.setManufacturer(command.manufacturer());
        asset.setStatus(AssetStatus.AVAILABLE);
        asset.setPurchaseDate(command.purchaseDate());
        asset.setWarrantyExpiry(command.warrantyExpiry());
        asset.setLocation(command.location());
        asset.setCostCenter(command.costCenter());
        asset.setNotes(command.notes());

        AssetEntity saved = assetRepository.save(asset);

        return AssetResponse.from(saved);
    }
}
