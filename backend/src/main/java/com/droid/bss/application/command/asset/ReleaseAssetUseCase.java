package com.droid.bss.application.command.asset;

import com.droid.bss.application.dto.asset.AssetResponse;
import com.droid.bss.domain.asset.AssetEntity;
import com.droid.bss.domain.asset.AssetRepository;
import com.droid.bss.infrastructure.metrics.BusinessMetrics;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use case for releasing assets
 */
@Service
public class ReleaseAssetUseCase {

    private final AssetRepository assetRepository;
    private final BusinessMetrics businessMetrics;

    public ReleaseAssetUseCase(AssetRepository assetRepository, BusinessMetrics businessMetrics) {
        this.assetRepository = assetRepository;
        this.businessMetrics = businessMetrics;
    }

    @Transactional
    public AssetResponse handle(String assetId) {
        AssetEntity asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new RuntimeException("Asset not found: " + assetId));

        // Check if asset is in use
        if (!asset.isInUse()) {
            throw new IllegalStateException("Asset is not in use: " + assetId);
        }

        // Release asset
        asset.release();

        AssetEntity saved = assetRepository.save(asset);

        return AssetResponse.from(saved);
    }
}
