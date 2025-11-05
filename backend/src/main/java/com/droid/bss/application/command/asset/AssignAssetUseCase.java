package com.droid.bss.application.command.asset;

import com.droid.bss.application.dto.asset.AssetResponse;
import com.droid.bss.application.dto.asset.AssignAssetCommand;
import com.droid.bss.domain.asset.AssetEntity;
import com.droid.bss.domain.asset.AssetRepository;
import com.droid.bss.domain.asset.AssetStatus;
import com.droid.bss.infrastructure.metrics.BusinessMetrics;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Use case for assigning assets
 */
@Service
public class AssignAssetUseCase {

    private final AssetRepository assetRepository;
    private final BusinessMetrics businessMetrics;

    public AssignAssetUseCase(AssetRepository assetRepository, BusinessMetrics businessMetrics) {
        this.assetRepository = assetRepository;
        this.businessMetrics = businessMetrics;
    }

    @Transactional
    public AssetResponse handle(String assetId, AssignAssetCommand command) {
        AssetEntity asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new RuntimeException("Asset not found: " + assetId));

        // Check if asset is available
        if (!asset.isAvailable()) {
            throw new IllegalStateException("Asset is not available: " + assetId);
        }

        // Assign asset
        asset.assignTo(command.assignedToType(), command.assignedToId(), command.assignedToName());

        AssetEntity saved = assetRepository.save(asset);

        return AssetResponse.from(saved);
    }
}
