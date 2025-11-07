package com.droid.bss.infrastructure.messaging.metrics;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Event Consumer Metrics
 *
 * Tracks metrics for a single event consumer
 */
public class EventConsumerMetrics {
    private final String consumerName;
    private final AtomicLong totalEventsProcessed = new AtomicLong(0);
    private final AtomicLong totalEventsFailed = new AtomicLong(0);
    private final AtomicLong totalEventsRetried = new AtomicLong(0);
    private final AtomicLong totalEventsSentToDLQ = new AtomicLong(0);
    private final AtomicLong lastUpdateTime = new AtomicLong(System.currentTimeMillis());

    public EventConsumerMetrics(String consumerName) {
        this.consumerName = consumerName;
    }

    public synchronized void update(long processed, long failed, long retried, long dlq) {
        totalEventsProcessed.addAndGet(processed);
        totalEventsFailed.addAndGet(failed);
        totalEventsRetried.addAndGet(retried);
        totalEventsSentToDLQ.addAndGet(dlq);
        lastUpdateTime.set(System.currentTimeMillis());
    }

    public String getConsumerName() {
        return consumerName;
    }

    public long getTotalEventsProcessed() {
        return totalEventsProcessed.get();
    }

    public long getTotalEventsFailed() {
        return totalEventsFailed.get();
    }

    public long getTotalEventsRetried() {
        return totalEventsRetried.get();
    }

    public long getTotalEventsSentToDLQ() {
        return totalEventsSentToDLQ.get();
    }

    public long getLastUpdateTime() {
        return lastUpdateTime.get();
    }

    public double getSuccessRate() {
        long total = totalEventsProcessed.get() + totalEventsFailed.get();
        if (total == 0) {
            return 0.0;
        }
        return (double) totalEventsProcessed.get() / total * 100.0;
    }

    public int getDuplicateEventCount() {
        // This would be populated by the actual consumer
        // For now, returning 0 as it's consumer-specific
        return 0;
    }
}
