package com.droid.bss.infrastructure.messaging.deadletter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Aggregates and reports DLQ statistics.
 *
 * @since 1.0
 */
public class DLQStatisticsAggregator {

    private static final Logger log = LoggerFactory.getLogger(DLQStatisticsAggregator.class);

    private final DeadLetterQueue deadLetterQueue;
    private final Map<String, Long> lastStats = new ConcurrentHashMap<>();

    /**
     * Creates a new DLQStatisticsAggregator.
     *
     * @param deadLetterQueue the DLQ
     */
    public DLQStatisticsAggregator(DeadLetterQueue deadLetterQueue) {
        this.deadLetterQueue = deadLetterQueue;
    }

    /**
     * Reports DLQ statistics periodically.
     */
    @Scheduled(fixedDelayString = "${app.dlq.statistics-interval-seconds:60}000")
    public void reportStatistics() {
        try {
            DLQStats stats = deadLetterQueue.getStats();

            log.info("DLQ Statistics: {}", stats.getSummary());

            // Log breakdown by topic
            Map<String, Long> byTopic = stats.getMessagesByTopic();
            if (!byTopic.isEmpty()) {
                log.info("DLQ Messages by Topic: {}", byTopic);
            }

            // Log breakdown by error type
            Map<String, Long> byErrorType = stats.getMessagesByErrorType();
            if (!byErrorType.isEmpty()) {
                log.info("DLQ Messages by Error Type: {}", byErrorType);
            }

            // Log requeue rate
            double requeueRate = stats.getRequeueRate();
            if (requeueRate > 0) {
                log.info("DLQ Requeue Rate: {:.2f}%", requeueRate);
            }

            // Log error rate
            double errorRate = stats.getErrorRate();
            if (errorRate > 0) {
                log.warn("DLQ Error Rate: {:.2f}%", errorRate);
            }

            lastStats.put("totalMessages", stats.getTotalMessages());
            lastStats.put("totalAdded", stats.getTotalAdded());
            lastStats.put("totalRequeued", stats.getTotalRequeued());
            lastStats.put("totalErrors", stats.getTotalErrors());

        } catch (Exception e) {
            log.error("Error reporting DLQ statistics: {}", e.getMessage(), e);
        }
    }

    /**
     * Gets the last statistics snapshot.
     *
     * @return the map of statistics
     */
    public Map<String, Long> getLastStatistics() {
        return new ConcurrentHashMap<>(lastStats);
    }
}
