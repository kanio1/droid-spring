package com.droid.bss.domain.asset;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Asset entities
 */
@Repository
public interface AssetRepository extends JpaRepository<AssetEntity, String> {

    /**
     * Find assets by status
     */
    List<AssetEntity> findByStatus(AssetStatus status);

    /**
     * Find assets by asset type
     */
    List<AssetEntity> findByAssetType(AssetType assetType);

    /**
     * Find assets assigned to a specific customer
     */
    @Query("SELECT a FROM AssetEntity a WHERE a.assignedToId = :customerId AND a.deletedAt IS NULL")
    List<AssetEntity> findByCustomerId(@Param("customerId") String customerId);

    /**
     * Find available assets (not assigned)
     */
    @Query("SELECT a FROM AssetEntity a WHERE a.status = 'AVAILABLE' AND a.deletedAt IS NULL")
    List<AssetEntity> findAvailable();

    /**
     * Find assets expiring warranty in next N days
     */
    @Query("SELECT a FROM AssetEntity a WHERE a.warrantyExpiry <= :endDate AND a.warrantyExpiry >= :startDate AND a.deletedAt IS NULL")
    List<AssetEntity> findWarrantyExpiringBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * Find assets by asset tag
     */
    Optional<AssetEntity> findByAssetTagAndDeletedAtIsNull(String assetTag);

    /**
     * Find assets by serial number
     */
    @Query("SELECT a FROM AssetEntity a WHERE a.serialNumber = :serialNumber AND a.deletedAt IS NULL")
    Optional<AssetEntity> findBySerialNumberAndDeletedAtIsNull(@Param("serialNumber") String serialNumber);

    /**
     * Count assets by type
     */
    @Query("SELECT a.assetType, COUNT(a) FROM AssetEntity a WHERE a.deletedAt IS NULL GROUP BY a.assetType")
    List<Object[]> countByAssetType();

    /**
     * Count assets by status
     */
    @Query("SELECT a.status, COUNT(a) FROM AssetEntity a WHERE a.deletedAt IS NULL GROUP BY a.status")
    List<Object[]> countByStatus();

    /**
     * Find assets in maintenance
     */
    @Query("SELECT a FROM AssetEntity a WHERE a.status = 'MAINTENANCE' AND a.deletedAt IS NULL")
    List<AssetEntity> findInMaintenance();

    /**
     * Find assets at location
     */
    @Query("SELECT a FROM AssetEntity a WHERE a.location = :location AND a.deletedAt IS NULL")
    List<AssetEntity> findByLocation(@Param("location") String location);

    /**
     * Find damaged assets
     */
    @Query("SELECT a FROM AssetEntity a WHERE a.status = 'DAMAGED' AND a.deletedAt IS NULL")
    List<AssetEntity> findDamaged();
}
