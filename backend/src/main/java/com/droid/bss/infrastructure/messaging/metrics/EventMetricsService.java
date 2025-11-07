package com.droid.bss.infrastructure.messaging.metrics;

import com.droid.bss.infrastructure.messaging.*;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Event Metrics Service
 *
 * Aggregates metrics from all event consumers for monitoring and observability
 */
@Service
public class EventMetricsService {

    private final Map<String, EventConsumerMetrics> consumerMetrics = new ConcurrentHashMap<>();
    private final AtomicLong totalEventsReceived = new AtomicLong(0);
    private final AtomicLong totalEventsProcessed = new AtomicLong(0);
    private final AtomicLong totalEventsFailed = new AtomicLong(0);
    private final AtomicLong totalEventsRetried = new AtomicLong(0);
    private final AtomicLong totalEventsSentToDLQ = new AtomicLong(0);

    private final long startTime = System.currentTimeMillis();

    /**
     * Register an event consumer for metrics collection
     */
    public void registerConsumer(String consumerName, Object consumer) {
        consumerMetrics.put(consumerName, new EventConsumerMetrics(consumerName));
    }

    /**
     * Update metrics from a consumer
     */
    public void updateMetrics(String consumerName, long processed, long failed, long retried, long dlq) {
        EventConsumerMetrics metrics = consumerMetrics.get(consumerName);
        if (metrics != null) {
            metrics.update(processed, failed, retried, dlq);

            // Update global metrics
            totalEventsProcessed.addAndGet(processed);
            totalEventsFailed.addAndGet(failed);
            totalEventsRetried.addAndGet(retried);
            totalEventsSentToDLQ.addAndGet(dlq);
        }
    }

    /**
     * Record an event reception
     */
    public void recordEventReceived() {
        totalEventsReceived.incrementAndGet();
    }

    /**
     * Get overall event processing statistics
     */
    public EventProcessingStatistics getOverallStatistics() {
        long uptime = System.currentTimeMillis() - startTime;
        long total = totalEventsProcessed.get() + totalEventsFailed.get();

        return new EventProcessingStatistics(
                totalEventsReceived.get(),
                totalEventsProcessed.get(),
                totalEventsFailed.get(),
                totalEventsRetried.get(),
                totalEventsSentToDLQ.get(),
                total,
                uptime,
                total == 0 ? 0.0 : (double) totalEventsProcessed.get() / total * 100.0,
                getEventsPerSecond()
        );
    }

    /**
     * Get metrics for all consumers
     */
    public Map<String, EventConsumerMetrics> getConsumerMetrics() {
        return new ConcurrentHashMap<>(consumerMetrics);
    }

    /**
     * Get consumer-specific metrics
     */
    public EventConsumerMetrics getConsumerMetrics(String consumerName) {
        return consumerMetrics.get(consumerName);
    }

    /**
     * Calculate events per second
     */
    private double getEventsPerSecond() {
        long uptime = (System.currentTimeMillis() - startTime) / 1000; // seconds
        if (uptime == 0) return 0.0;
        return (double) totalEventsProcessed.get() / uptime;
    }

    /**
     * Get health status
     */
    public EventHealthStatus getHealthStatus() {
        long total = totalEventsProcessed.get() + totalEventsFailed.get();
        if (total == 0) {
            return new EventHealthStatus("UNKNOWN", "No events processed yet", 0.0);
        }

        double successRate = (double) totalEventsProcessed.get() / total * 100.0;
        String status;
        String message;

        if (successRate >= 99.0) {
            status = "HEALTHY";
            message = "All systems operational";
        } else if (successRate >= 95.0) {
            status = "DEGRADED";
            message = "Elevated failure rate detected";
        } else if (successRate >= 90.0) {
            status = "UNHEALTHY";
            message = "High failure rate detected";
        } else {
            status = "CRITICAL";
            message = "Critical failure rate - immediate attention required";
        }

        return new EventHealthStatus(status, message, successRate);
    }
}
