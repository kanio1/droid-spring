package com.droid.bss.domain.asset;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for NetworkElement entities
 */
@Repository
public interface NetworkElementRepository extends JpaRepository<NetworkElementEntity, String> {

    /**
     * Find network elements by element type
     */
    List<NetworkElementEntity> findByElementType(NetworkElementType elementType);

    /**
     * Find network elements by status
     */
    List<NetworkElementEntity> findByStatus(AssetStatus status);

    /**
     * Find network element by element ID
     */
    Optional<NetworkElementEntity> findByElementIdAndDeletedAtIsNull(String elementId);

    /**
     * Find online network elements (with recent heartbeat)
     */
    @Query("SELECT ne FROM NetworkElementEntity ne WHERE ne.lastHeartbeat > :threshold AND ne.deletedAt IS NULL")
    List<NetworkElementEntity> findOnlineElements(@Param("threshold") LocalDateTime threshold);

    /**
     * Find elements needing heartbeat (offline or stale)
     */
    @Query("SELECT ne FROM NetworkElementEntity ne WHERE ne.lastHeartbeat < :threshold AND ne.deletedAt IS NULL")
    List<NetworkElementEntity> findElementsNeedingHeartbeat(@Param("threshold") LocalDateTime threshold);

    /**
     * Find elements in maintenance mode
     */
    @Query("SELECT ne FROM NetworkElementEntity ne WHERE ne.maintenanceMode = true AND ne.deletedAt IS NULL")
    List<NetworkElementEntity> findInMaintenance();

    /**
     * Find elements at location
     */
    @Query("SELECT ne FROM NetworkElementEntity ne WHERE ne.location = :location AND ne.deletedAt IS NULL")
    List<NetworkElementEntity> findByLocation(@Param("location") String location);

    /**
     * Count elements by type
     */
    @Query("SELECT ne.elementType, COUNT(ne) FROM NetworkElementEntity ne WHERE ne.deletedAt IS NULL GROUP BY ne.elementType")
    List<Object[]> countByElementType();

    /**
     * Count elements by status
     */
    @Query("SELECT ne.status, COUNT(ne) FROM NetworkElementEntity ne WHERE ne.deletedAt IS NULL GROUP BY ne.status")
    List<Object[]> countByStatus();

    /**
     * Find elements with IP address
     */
    @Query("SELECT ne FROM NetworkElementEntity ne WHERE ne.ipAddress = :ipAddress AND ne.deletedAt IS NULL")
    List<NetworkElementEntity> findByIpAddress(@Param("ipAddress") String ipAddress);

    /**
     * Find elements by rack position
     */
    @Query("SELECT ne FROM NetworkElementEntity ne WHERE ne.rackPosition = :rackPosition AND ne.deletedAt IS NULL")
    List<NetworkElementEntity> findByRackPosition(@Param("rackPosition") String rackPosition);
}
