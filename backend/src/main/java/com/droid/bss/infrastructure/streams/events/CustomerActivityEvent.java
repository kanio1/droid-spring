package com.droid.bss.infrastructure.streams.events;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Customer activity event for Kafka Streams
 */
public class CustomerActivityEvent {
    private UUID customerId;
    private String activityType;
    private Map<String, Object> metadata;
    private Instant timestamp;
    private String sessionId;

    // Getters and setters
    public UUID getCustomerId() { return customerId; }
    public void setCustomerId(UUID customerId) { this.customerId = customerId; }
    public String getActivityType() { return activityType; }
    public void setActivityType(String activityType) { this.activityType = activityType; }
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    // Builder
    public static class Builder {
        private CustomerActivityEvent event = new CustomerActivityEvent();

        public Builder customerId(UUID customerId) {
            event.customerId = customerId;
            return this;
        }

        public Builder activityType(String activityType) {
            event.activityType = activityType;
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            event.metadata = metadata;
            return this;
        }

        public Builder timestamp(Instant timestamp) {
            event.timestamp = timestamp;
            return this;
        }

        public Builder sessionId(String sessionId) {
            event.sessionId = sessionId;
            return this;
        }

        public CustomerActivityEvent build() {
            return event;
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}
