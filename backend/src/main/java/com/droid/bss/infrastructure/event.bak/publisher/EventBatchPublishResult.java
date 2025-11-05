package com.droid.bss.infrastructure.event.publisher;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Result of a batch event publishing operation.
 *
 * @since 1.0
 */
public class EventBatchPublishResult {

    private final boolean success;
    private final int totalEvents;
    private final int successCount;
    private final int failureCount;
    private final List<EventPublishResult> results;
    private final Instant completedAt;
    private final Duration processingTime;
    private final Map<String, Integer> eventsByTopic;

    private EventBatchPublishResult(Builder builder) {
        this.success = builder.success;
        this.totalEvents = builder.totalEvents;
        this.successCount = builder.successCount;
        this.failureCount = builder.failureCount;
        this.results = List.copyOf(builder.results);
        this.completedAt = builder.completedAt;
        this.processingTime = builder.processingTime;
        this.eventsByTopic = Map.copyOf(builder.eventsByTopic);
    }

    /**
     * Creates a successful batch publish result.
     *
     * @param results the individual publish results
     * @param processingTime the time taken to process the batch
     * @return the batch result
     */
    public static EventBatchPublishResult success(List<EventPublishResult> results, Duration processingTime) {
        int total = results.size();
        int success = (int) results.stream().filter(EventPublishResult::isSuccess).count();
        int failure = total - success;

        Map<String, Integer> eventsByTopic = results.stream()
            .filter(EventPublishResult::isSuccess)
            .collect(Collectors.groupingBy(
                EventPublishResult::getTopic,
                HashMap::new,
                Collectors.summingInt(r -> 1)
            ));

        return builder()
            .success(true)
            .totalEvents(total)
            .successCount(success)
            .failureCount(failure)
            .results(results)
            .processingTime(processingTime)
            .completedAt(Instant.now())
            .eventsByTopic(eventsByTopic)
            .build();
    }

    /**
     * Creates a failed batch publish result.
     *
     * @param totalEvents the total number of events
     * @param results the individual publish results
     * @param processingTime the time taken to process the batch
     * @return the batch result
     */
    public static EventBatchPublishResult failure(int totalEvents, List<EventPublishResult> results,
                                                  Duration processingTime) {
        int failure = (int) results.stream().filter(r -> !r.isSuccess()).count();

        return builder()
            .success(false)
            .totalEvents(totalEvents)
            .successCount(totalEvents - failure)
            .failureCount(failure)
            .results(results)
            .processingTime(processingTime)
            .completedAt(Instant.now())
            .eventsByTopic(Map.of())
            .build();
    }

    /**
     * Checks if the batch publish operation was successful.
     *
     * @return true if all events were published successfully
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Gets the total number of events in the batch.
     *
     * @return the total events
     */
    public int getTotalEvents() {
        return totalEvents;
    }

    /**
     * Gets the number of successfully published events.
     *
     * @return the success count
     */
    public int getSuccessCount() {
        return successCount;
    }

    /**
     * Gets the number of failed event publications.
     *
     * @return the failure count
     */
    public int getFailureCount() {
        return failureCount;
    }

    /**
     * Gets the individual publish results.
     *
     * @return the list of results (unmodifiable)
     */
    public List<EventPublishResult> getResults() {
        return results;
    }

    /**
     * Gets the time when batch processing completed.
     *
     * @return the completion timestamp
     */
    public Instant getCompletedAt() {
        return completedAt;
    }

    /**
     * Gets the processing time.
     *
     * @return the processing duration
     */
    public Duration getProcessingTime() {
        return processingTime;
    }

    /**
     * Gets the distribution of events by topic.
     *
     * @return the map of topic to event count
     */
    public Map<String, Integer> getEventsByTopic() {
        return eventsByTopic;
    }

    /**
     * Gets the failed event results.
     *
     * @return the list of failed results (unmodifiable)
     */
    public List<EventPublishResult> getFailedResults() {
        return results.stream()
            .filter(r -> !r.isSuccess())
            .collect(Collectors.toUnmodifiableList());
    }

    /**
     * Gets the successful event results.
     *
     * @return the list of successful results (unmodifiable)
     */
    public List<EventPublishResult> getSuccessResults() {
        return results.stream()
            .filter(EventPublishResult::isSuccess)
            .collect(Collectors.toUnmodifiableList());
    }

    /**
     * Creates a builder for EventBatchPublishResult.
     *
     * @return the builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for EventBatchPublishResult.
     */
    public static class Builder {
        private boolean success;
        private int totalEvents;
        private int successCount;
        private int failureCount;
        private List<EventPublishResult> results = new ArrayList<>();
        private Instant completedAt;
        private Duration processingTime;
        private Map<String, Integer> eventsByTopic = new HashMap<>();

        private Builder() {}

        public Builder success(boolean success) {
            this.success = success;
            return this;
        }

        public Builder totalEvents(int totalEvents) {
            this.totalEvents = totalEvents;
            return this;
        }

        public Builder successCount(int successCount) {
            this.successCount = successCount;
            return this;
        }

        public Builder failureCount(int failureCount) {
            this.failureCount = failureCount;
            return this;
        }

        public Builder results(List<EventPublishResult> results) {
            this.results = results != null ? results : Collections.emptyList();
            return this;
        }

        public Builder completedAt(Instant completedAt) {
            this.completedAt = completedAt;
            return this;
        }

        public Builder processingTime(Duration processingTime) {
            this.processingTime = processingTime;
            return this;
        }

        public Builder eventsByTopic(Map<String, Integer> eventsByTopic) {
            this.eventsByTopic = eventsByTopic != null ? eventsByTopic : new HashMap<>();
            return this;
        }

        public EventBatchPublishResult build() {
            return new EventBatchPublishResult(this);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EventBatchPublishResult that = (EventBatchPublishResult) o;
        return success == that.success &&
            totalEvents == that.totalEvents &&
            successCount == that.successCount &&
            failureCount == that.failureCount &&
            Objects.equals(results, that.results) &&
            Objects.equals(completedAt, that.completedAt) &&
            Objects.equals(processingTime, that.processingTime) &&
            Objects.equals(eventsByTopic, that.eventsByTopic);
    }

    @Override
    public int hashCode() {
        return Objects.hash(success, totalEvents, successCount, failureCount, results, completedAt, processingTime, eventsByTopic);
    }

    @Override
    public String toString() {
        return "EventBatchPublishResult{" +
            "success=" + success +
            ", totalEvents=" + totalEvents +
            ", successCount=" + successCount +
            ", failureCount=" + failureCount +
            ", processingTime=" + processingTime +
            ", eventsByTopic=" + eventsByTopic +
            '}';
    }
}
