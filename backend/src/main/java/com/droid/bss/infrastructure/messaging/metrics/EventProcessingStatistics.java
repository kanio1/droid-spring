package com.droid.bss.infrastructure.messaging.metrics;

/**
 * Overall Event Processing Statistics
 */
public class EventProcessingStatistics {
    private final long totalEventsReceived;
    private final long totalEventsProcessed;
    private final long totalEventsFailed;
    private final long totalEventsRetried;
    private final long totalEventsSentToDLQ;
    private final long totalEvents;
    private final long uptimeMillis;
    private final double successRate;
    private final double eventsPerSecond;

    public EventProcessingStatistics(
            long totalEventsReceived,
            long totalEventsProcessed,
            long totalEventsFailed,
            long totalEventsRetried,
            long totalEventsSentToDLQ,
            long totalEvents,
            long uptimeMillis,
            double successRate,
            double eventsPerSecond
    ) {
        this.totalEventsReceived = totalEventsReceived;
        this.totalEventsProcessed = totalEventsProcessed;
        this.totalEventsFailed = totalEventsFailed;
        this.totalEventsRetried = totalEventsRetried;
        this.totalEventsSentToDLQ = totalEventsSentToDLQ;
        this.totalEvents = totalEvents;
        this.uptimeMillis = uptimeMillis;
        this.successRate = successRate;
        this.eventsPerSecond = eventsPerSecond;
    }

    public long getTotalEventsReceived() { return totalEventsReceived; }
    public long getTotalEventsProcessed() { return totalEventsProcessed; }
    public long getTotalEventsFailed() { return totalEventsFailed; }
    public long getTotalEventsRetried() { return totalEventsRetried; }
    public long getTotalEventsSentToDLQ() { return totalEventsSentToDLQ; }
    public long getTotalEvents() { return totalEvents; }
    public long getUptimeMillis() { return uptimeMillis; }
    public double getSuccessRate() { return successRate; }
    public double getEventsPerSecond() { return eventsPerSecond; }

    public String getUptimeFormatted() {
        long seconds = uptimeMillis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) {
            return String.format("%d days, %d hours, %d minutes", days, hours % 24, minutes % 60);
        } else if (hours > 0) {
            return String.format("%d hours, %d minutes", hours, minutes % 60);
        } else if (minutes > 0) {
            return String.format("%d minutes, %d seconds", minutes, seconds % 60);
        } else {
            return String.format("%d seconds", seconds);
        }
    }
}
