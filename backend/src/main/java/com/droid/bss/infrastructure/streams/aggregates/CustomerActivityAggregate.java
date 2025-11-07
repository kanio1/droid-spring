package com.droid.bss.infrastructure.streams.aggregates;

import com.droid.bss.infrastructure.streams.events.CustomerActivityEvent;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Aggregate for customer activity tracking
 */
public class CustomerActivityAggregate {
    private UUID customerId;
    private List<CustomerActivityEvent> activities;
    private Long sessionCount;
    private Duration totalDuration;
    private Instant lastActivityTime;
    private Map<String, Integer> activityCounts;

    public CustomerActivityAggregate() {
        this.activities = new ArrayList<>();
        this.sessionCount = 0L;
        this.totalDuration = Duration.ofSeconds(0);
        this.activityCounts = new HashMap<>();
    }

    public CustomerActivityAggregate addEvent(CustomerActivityEvent event) {
        activities.add(event);
        sessionCount++;
        lastActivityTime = event.getTimestamp();

        // Count activity types
        activityCounts.merge(event.getActivityType(), 1, Integer::sum);

        return this;
    }

    // Getters and setters
    public UUID getCustomerId() {
        return customerId != null ? customerId :
            (activities.isEmpty() ? null : activities.get(0).getCustomerId());
    }

    public void setCustomerId(UUID customerId) {
        this.customerId = customerId;
    }

    public List<CustomerActivityEvent> getActivities() { return activities; }
    public void setActivities(List<CustomerActivityEvent> activities) { this.activities = activities; }
    public Long getSessionCount() { return sessionCount; }
    public void setSessionCount(Long sessionCount) { this.sessionCount = sessionCount; }
    public Duration getTotalDuration() { return totalDuration; }
    public void setTotalDuration(Duration totalDuration) { this.totalDuration = totalDuration; }
    public Instant getLastActivityTime() { return lastActivityTime; }
    public void setLastActivityTime(Instant lastActivityTime) { this.lastActivityTime = lastActivityTime; }
    public Map<String, Integer> getActivityCounts() { return activityCounts; }
    public void setActivityCounts(Map<String, Integer> activityCounts) { this.activityCounts = activityCounts; }
}
