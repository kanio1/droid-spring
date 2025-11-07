package com.droid.bss.infrastructure.write.monitoring;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for ResourceThresholdEntity
 */
@Repository
public interface SpringDataResourceThresholdRepository extends JpaRepository<ResourceThresholdEntity, Long> {

    /**
     * Find thresholds by customer ID
     */
    List<ResourceThresholdEntity> findByCustomerId(Long customerId);

    /**
     * Find thresholds by resource ID
     */
    List<ResourceThresholdEntity> findByResourceId(Long resourceId);

    /**
     * Find thresholds by customer ID and resource ID
     */
    List<ResourceThresholdEntity> findByCustomerIdAndResourceId(Long customerId, Long resourceId);

    /**
     * Find threshold by customer ID, resource ID, and metric type
     */
    Optional<ResourceThresholdEntity> findByCustomerIdAndResourceIdAndMetricType(
            Long customerId, Long resourceId, String metricType);

    /**
     * Find all enabled thresholds
     */
    List<ResourceThresholdEntity> findByEnabledTrue();

    /**
     * Check if a threshold exists for customer, resource, and metric type
     */
    boolean existsByCustomerIdAndResourceIdAndMetricType(
            Long customerId, Long resourceId, String metricType);
}
