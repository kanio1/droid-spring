package com.droid.bss.domain.service;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for ServiceEntity
 */
@Repository
public interface ServiceRepository extends JpaRepository<ServiceEntity, String> {

    /**
     * Find active service by service code
     */
    @Query("SELECT s FROM ServiceEntity s WHERE s.serviceCode = :serviceCode AND s.status = 'ACTIVE'")
    Optional<ServiceEntity> findActiveByServiceCode(@Param("serviceCode") String serviceCode);

    /**
     * Find all active services
     */
    @Query("SELECT s FROM ServiceEntity s WHERE s.status = 'ACTIVE' ORDER BY s.name")
    List<ServiceEntity> findAllActive();

    /**
     * Find services by category
     */
    @Query("SELECT s FROM ServiceEntity s WHERE s.category = :category AND s.status = 'ACTIVE' ORDER BY s.name")
    List<ServiceEntity> findByCategory(@Param("category") String category);

    /**
     * Find services by type
     */
    @Query("SELECT s FROM ServiceEntity s WHERE s.serviceType = :serviceType AND s.status = 'ACTIVE' ORDER BY s.name")
    List<ServiceEntity> findByServiceType(@Param("serviceType") ServiceType serviceType);

    /**
     * Check if service code exists
     */
    @Query("SELECT COUNT(s) > 0 FROM ServiceEntity s WHERE s.serviceCode = :serviceCode")
    boolean existsByServiceCode(@Param("serviceCode") String serviceCode);

    /**
     * Find services that don't depend on the given service
     */
    @Query("SELECT s FROM ServiceEntity s WHERE :serviceCode NOT MEMBER OF s.dependsOnServiceCodes")
    List<ServiceEntity> findWithoutDependency(@Param("serviceCode") String serviceCode);
}
