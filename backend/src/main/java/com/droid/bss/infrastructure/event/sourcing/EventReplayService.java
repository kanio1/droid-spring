package com.droid.bss.infrastructure.event.sourcing;

import io.cloudevents.CloudEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

/**
 * Service for replaying events
 */
@Service
public class EventReplayService {

    private final EventStore eventStore;
    private final EventSerializer serializer;
    private final Executor replayExecutor = ForkJoinPool.commonPool();

    public EventReplayService(EventStore eventStore, EventSerializer serializer) {
        this.eventStore = eventStore;
        this.serializer = serializer;
    }

    /**
     * Replay all events for a specific aggregate
     */
    @Transactional(readOnly = true)
    public ReplayResult replayAggregateEvents(String aggregateId) {
        List<StoredEvent> events = eventStore.getEventsForAggregate(aggregateId);

        if (events.isEmpty()) {
            return new ReplayResult(0, 0, "No events found for aggregate: " + aggregateId);
        }

        return replayEvents(events);
    }

    /**
     * Replay events by type
     */
    @Transactional(readOnly = true)
    public ReplayResult replayEventsByType(String eventType) {
        List<StoredEvent> events = eventStore.getEventsByType(eventType);

        if (events.isEmpty()) {
            return new ReplayResult(0, 0, "No events found for type: " + eventType);
        }

        return replayEvents(events);
    }

    /**
     * Replay events by correlation ID
     */
    @Transactional(readOnly = true)
    public ReplayResult replayEventsByCorrelationId(String correlationId) {
        List<StoredEvent> events = eventStore.getEventsByCorrelationId(correlationId);

        if (events.isEmpty()) {
            return new ReplayResult(0, 0, "No events found for correlation ID: " + correlationId);
        }

        return replayEvents(events);
    }

    /**
     * Replay events in a time range
     */
    @Transactional(readOnly = true)
    public ReplayResult replayEventsInTimeRange(LocalDateTime start, LocalDateTime end) {
        // This would require a query by time range
        // For now, we'll return all events
        List<StoredEvent> allEvents = eventStore.getEventsSince(UUID.randomUUID());
        List<StoredEvent> filteredEvents = allEvents.stream()
                .filter(event -> {
                    LocalDateTime eventTime = event.getTimestamp();
                    return !eventTime.isBefore(start) && !eventTime.isAfter(end);
                })
                .collect(Collectors.toList());

        if (filteredEvents.isEmpty()) {
            return new ReplayResult(0, 0, "No events found in time range: " + start + " to " + end);
        }

        return replayEvents(filteredEvents);
    }

    /**
     * Replay all events since a specific event
     */
    @Transactional(readOnly = true)
    public ReplayResult replayEventsSince(UUID eventId) {
        List<StoredEvent> events = eventStore.getEventsSince(eventId);

        if (events.isEmpty()) {
            return new ReplayResult(0, 0, "No events found since: " + eventId);
        }

        return replayEvents(events);
    }

    /**
     * Async replay of events
     */
    public CompletableFuture<ReplayResult> replayEventsAsync(String aggregateId) {
        return CompletableFuture.supplyAsync(() -> replayAggregateEvents(aggregateId), replayExecutor);
    }

    /**
     * Batch replay for multiple aggregates
     */
    @Transactional(readOnly = true)
    public Map<String, ReplayResult> replayMultipleAggregates(List<String> aggregateIds) {
        return aggregateIds.parallelStream()
                .collect(Collectors.toMap(
                        id -> id,
                        this::replayAggregateEvents
                ));
    }

    /**
     * Validate event stream integrity
     */
    public IntegrityCheckResult checkEventStreamIntegrity(String aggregateId) {
        List<StoredEvent> events = eventStore.getEventsForAggregate(aggregateId);

        if (events.isEmpty()) {
            return new IntegrityCheckResult(true, 0, 0, "No events to check");
        }

        Map<Long, String> versionToEvent = new HashMap<>();
        long invalidVersions = 0;

        for (StoredEvent event : events) {
            long version = event.getVersion();
            if (version <= 0) {
                invalidVersions++;
            } else if (versionToEvent.containsKey(version)) {
                invalidVersions++;
            } else {
                versionToEvent.put(version, event.getEventType());
            }
        }

        boolean isValid = invalidVersions == 0;
        String message = isValid
                ? String.format("Event stream is valid. Total events: %d", events.size())
                : String.format("Event stream has %d invalid versions", invalidVersions);

        return new IntegrityCheckResult(isValid, events.size(), invalidVersions, message);
    }

    /**
     * Get event statistics
     */
    @Transactional(readOnly = true)
    public EventStatistics getEventStatistics() {
        // This would typically query the database directly for statistics
        // For now, return a placeholder
        return new EventStatistics(
                0, // total events
                0, // unique aggregates
                new HashMap<>() // events by type
        );
    }

    /**
     * Replay a list of events
     */
    private ReplayResult replayEvents(List<StoredEvent> events) {
        long successCount = 0;
        long failureCount = 0;
        StringBuilder report = new StringBuilder();

        for (StoredEvent event : events) {
            try {
                // Reconstruct CloudEvent
                CloudEvent cloudEvent = serializer.toCloudEvent(event);

                // Log the event (in a real implementation, you might apply it to a test aggregate)
                report.append(String.format("Replayed: %s for aggregate %s at version %d\n",
                        event.getEventType(),
                        event.getAggregateId(),
                        event.getVersion()));

                successCount++;
            } catch (Exception e) {
                failureCount++;
                report.append(String.format("Failed to replay event: %s - %s\n",
                        event.getEventType(),
                        e.getMessage()));
            }
        }

        return new ReplayResult(successCount, failureCount, report.toString());
    }

    /**
     * Result of event replay operation
     */
    public static class ReplayResult {
        private final long successCount;
        private final long failureCount;
        private final String report;

        public ReplayResult(long successCount, long failureCount, String report) {
            this.successCount = successCount;
            this.failureCount = failureCount;
            this.report = report;
        }

        public long getSuccessCount() {
            return successCount;
        }

        public long getFailureCount() {
            return failureCount;
        }

        public long getTotalCount() {
            return successCount + failureCount;
        }

        public String getReport() {
            return report;
        }
    }

    /**
     * Result of integrity check
     */
    public static class IntegrityCheckResult {
        private final boolean valid;
        private final long totalEvents;
        private final long invalidVersions;
        private final String message;

        public IntegrityCheckResult(boolean valid, long totalEvents, long invalidVersions, String message) {
            this.valid = valid;
            this.totalEvents = totalEvents;
            this.invalidVersions = invalidVersions;
            this.message = message;
        }

        public boolean isValid() {
            return valid;
        }

        public long getTotalEvents() {
            return totalEvents;
        }

        public long getInvalidVersions() {
            return invalidVersions;
        }

        public String getMessage() {
            return message;
        }
    }

    /**
     * Event statistics
     */
    public static class EventStatistics {
        private final long totalEvents;
        private final long uniqueAggregates;
        private final Map<String, Long> eventsByType;

        public EventStatistics(long totalEvents, long uniqueAggregates, Map<String, Long> eventsByType) {
            this.totalEvents = totalEvents;
            this.uniqueAggregates = uniqueAggregates;
            this.eventsByType = eventsByType;
        }

        public long getTotalEvents() {
            return totalEvents;
        }

        public long getUniqueAggregates() {
            return uniqueAggregates;
        }

        public Map<String, Long> getEventsByType() {
            return eventsByType;
        }
    }
}
