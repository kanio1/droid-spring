package com.droid.bss.domain.monitoring;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository port for Alert
 */
public interface AlertRepository {

    /**
     * Save an alert
     */
    Alert save(Alert alert);

    /**
     * Find an alert by ID
     */
    Optional<Alert> findById(Long id);

    /**
     * Find alerts by customer ID
     */
    List<Alert> findByCustomerId(Long customerId);

    /**
     * Find alerts by resource ID
     */
    List<Alert> findByResourceId(Long resourceId);

    /**
     * Find alerts by customer ID and resource ID
     */
    List<Alert> findByCustomerIdAndResourceId(Long customerId, Long resourceId);

    /**
     * Find active alerts (OPEN or ACKNOWLEDGED)
     */
    List<Alert> findByStatusIn(List<String> statuses);

    /**
     * Find alerts by severity
     */
    List<Alert> findBySeverity(String severity);

    /**
     * Find alerts triggered within time range
     */
    List<Alert> findByTriggeredAtBetween(Instant startTime, Instant endTime);

    /**
     * Find open alerts for a specific resource and metric type
     */
    List<Alert> findByCustomerIdAndResourceIdAndMetricTypeAndStatusIn(
            Long customerId, Long resourceId, String metricType, List<String> statuses);

    /**
     * Count active alerts for a customer
     */
    long countByCustomerIdAndStatusIn(Long customerId, List<String> statuses);

    /**
     * Delete an alert by ID
     */
    void deleteById(Long id);
}
