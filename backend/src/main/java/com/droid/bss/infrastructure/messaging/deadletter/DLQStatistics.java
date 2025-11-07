package com.droid.bss.infrastructure.messaging.deadletter;

/**
 * Dead Letter Queue Statistics
 */
public class DLQStatistics {
    private final long totalFailedEvents;
    private final long totalReprocessedEvents;
    private final int currentFailedEvents;
    private final long pendingEvents;
    private final long resolvedEvents;

    public DLQStatistics(long totalFailedEvents, long totalReprocessedEvents,
                        int currentFailedEvents, long pendingEvents, long resolvedEvents) {
        this.totalFailedEvents = totalFailedEvents;
        this.totalReprocessedEvents = totalReprocessedEvents;
        this.currentFailedEvents = currentFailedEvents;
        this.pendingEvents = pendingEvents;
        this.resolvedEvents = resolvedEvents;
    }

    public long getTotalFailedEvents() { return totalFailedEvents; }
    public long getTotalReprocessedEvents() { return totalReprocessedEvents; }
    public int getCurrentFailedEvents() { return currentFailedEvents; }
    public long getPendingEvents() { return pendingEvents; }
    public long getResolvedEvents() { return resolvedEvents; }

    public double getReprocessSuccessRate() {
        if (totalFailedEvents == 0) return 0.0;
        return (double) totalReprocessedEvents / totalFailedEvents * 100.0;
    }
}
