package com.droid.bss.domain.monitoring;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository port for ResourceMetric
 */
public interface ResourceMetricRepository {

    /**
     * Save a resource metric
     */
    ResourceMetric save(ResourceMetric metric);

    /**
     * Save multiple metrics in batch
     */
    void saveAll(List<ResourceMetric> metrics);

    /**
     * Find a metric by ID
     */
    Optional<ResourceMetric> findById(Long id);

    /**
     * Find metrics by customer ID and time range
     */
    List<ResourceMetric> findByCustomerIdAndTimestampBetween(
            Long customerId, Instant startTime, Instant endTime);

    /**
     * Find metrics by resource ID and time range
     */
    List<ResourceMetric> findByResourceIdAndTimestampBetween(
            Long resourceId, Instant startTime, Instant endTime);

    /**
     * Find metrics by customer ID, resource ID, and time range
     */
    List<ResourceMetric> findByCustomerIdAndResourceIdAndTimestampBetween(
            Long customerId, Long resourceId, Instant startTime, Instant endTime);

    /**
     * Find latest metrics for a resource
     */
    List<ResourceMetric> findLatestByResourceId(Long resourceId, int limit);

    /**
     * Delete metrics older than specified time
     */
    void deleteByTimestampBefore(Instant cutoffTime);
}
