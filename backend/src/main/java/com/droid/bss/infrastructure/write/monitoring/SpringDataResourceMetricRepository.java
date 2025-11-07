package com.droid.bss.infrastructure.write.monitoring;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * Spring Data JPA repository for ResourceMetricEntity
 */
@Repository
public interface SpringDataResourceMetricRepository extends JpaRepository<ResourceMetricEntity, Long> {

    /**
     * Find metrics by customer ID and time range
     */
    List<ResourceMetricEntity> findByCustomerIdAndTimestampBetween(
            Long customerId, Instant startTime, Instant endTime);

    /**
     * Find metrics by resource ID and time range
     */
    List<ResourceMetricEntity> findByResourceIdAndTimestampBetween(
            Long resourceId, Instant startTime, Instant endTime);

    /**
     * Find metrics by customer ID, resource ID, and time range
     */
    List<ResourceMetricEntity> findByCustomerIdAndResourceIdAndTimestampBetween(
            Long customerId, Long resourceId, Instant startTime, Instant endTime);

    /**
     * Find top N latest metrics for a resource
     */
    List<ResourceMetricEntity> findTopByResourceIdOrderByTimestampDesc(Long resourceId, Pageable pageable);

    /**
     * Delete metrics older than specified time
     */
    void deleteByTimestampBefore(Instant cutoffTime);
}
