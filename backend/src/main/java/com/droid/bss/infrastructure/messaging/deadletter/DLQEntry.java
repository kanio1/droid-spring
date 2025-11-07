package com.droid.bss.infrastructure.messaging.deadletter;

import io.cloudevents.CloudEvent;

import java.time.LocalDateTime;

/**
 * Dead Letter Queue Entry
 *
 * Represents a single failed event stored in the DLQ
 */
public class DLQEntry {
    private final String eventId;
    private final String eventType;
    private final CloudEvent event;
    private final String errorMessage;
    private final String originalTopic;
    private final int partition;
    private final long offset;
    private final int retryCount;
    private final LocalDateTime failedAt;

    private DLQStatus status;
    private LocalDateTime reprocessedAt;
    private LocalDateTime resolvedAt;
    private String resolution;

    public DLQEntry(String eventId, String eventType, CloudEvent event,
                    String errorMessage, String originalTopic, int partition,
                    long offset, int retryCount, LocalDateTime failedAt,
                    DLQStatus status) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.event = event;
        this.errorMessage = errorMessage;
        this.originalTopic = originalTopic;
        this.partition = partition;
        this.offset = offset;
        this.retryCount = retryCount;
        this.failedAt = failedAt;
        this.status = status;
    }

    // Getters and setters
    public String getEventId() { return eventId; }
    public String getEventType() { return eventType; }
    public CloudEvent getEvent() { return event; }
    public String getErrorMessage() { return errorMessage; }
    public String getOriginalTopic() { return originalTopic; }
    public int getPartition() { return partition; }
    public long getOffset() { return offset; }
    public int getRetryCount() { return retryCount; }
    public LocalDateTime getFailedAt() { return failedAt; }
    public DLQStatus getStatus() { return status; }
    public void setStatus(DLQStatus status) { this.status = status; }
    public LocalDateTime getReprocessedAt() { return reprocessedAt; }
    public void setReprocessedAt(LocalDateTime reprocessedAt) { this.reprocessedAt = reprocessedAt; }
    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }
    public String getResolution() { return resolution; }
    public void setResolution(String resolution) { this.resolution = resolution; }
}
