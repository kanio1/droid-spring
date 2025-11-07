package com.droid.bss.domain.monitoring;

import java.util.List;
import java.util.Optional;

/**
 * Repository port for ResourceThreshold
 */
public interface ResourceThresholdRepository {

    /**
     * Save a threshold
     */
    ResourceThreshold save(ResourceThreshold threshold);

    /**
     * Find a threshold by ID
     */
    Optional<ResourceThreshold> findById(Long id);

    /**
     * Find thresholds by customer ID
     */
    List<ResourceThreshold> findByCustomerId(Long customerId);

    /**
     * Find thresholds by resource ID
     */
    List<ResourceThreshold> findByResourceId(Long resourceId);

    /**
     * Find thresholds by customer ID and resource ID
     */
    List<ResourceThreshold> findByCustomerIdAndResourceId(Long customerId, Long resourceId);

    /**
     * Find thresholds by customer ID, resource ID, and metric type
     */
    Optional<ResourceThreshold> findByCustomerIdAndResourceIdAndMetricType(
            Long customerId, Long resourceId, String metricType);

    /**
     * Find all enabled thresholds
     */
    List<ResourceThreshold> findByEnabledTrue();

    /**
     * Delete a threshold by ID
     */
    void deleteById(Long id);

    /**
     * Check if a threshold exists for customer, resource, and metric type
     */
    boolean existsByCustomerIdAndResourceIdAndMetricType(
            Long customerId, Long resourceId, String metricType);
}
