package com.droid.bss.infrastructure.write.monitoring;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * Spring Data JPA repository for AlertEntity
 */
@Repository
public interface SpringDataAlertRepository extends JpaRepository<AlertEntity, Long> {

    /**
     * Find alerts by customer ID
     */
    List<AlertEntity> findByCustomerId(Long customerId);

    /**
     * Find alerts by resource ID
     */
    List<AlertEntity> findByResourceId(Long resourceId);

    /**
     * Find alerts by customer ID and resource ID
     */
    List<AlertEntity> findByCustomerIdAndResourceId(Long customerId, Long resourceId);

    /**
     * Find alerts by status
     */
    List<AlertEntity> findByStatusIn(List<String> statuses);

    /**
     * Find alerts by severity
     */
    List<AlertEntity> findBySeverity(String severity);

    /**
     * Find alerts triggered within time range
     */
    List<AlertEntity> findByTriggeredAtBetween(Instant startTime, Instant endTime);

    /**
     * Find open alerts for a specific resource and metric type
     */
    List<AlertEntity> findByCustomerIdAndResourceIdAndMetricTypeAndStatusIn(
            Long customerId, Long resourceId, String metricType, List<String> statuses);

    /**
     * Count active alerts for a customer
     */
    long countByCustomerIdAndStatusIn(Long customerId, List<String> statuses);
}
