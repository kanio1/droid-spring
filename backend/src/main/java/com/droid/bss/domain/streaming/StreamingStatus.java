package com.droid.bss.domain.streaming;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

/**
 * Overall streaming status
 */
@Data
@Builder
public class StreamingStatus {

    private Long totalEvents;
    private Long pendingEvents;
    private Long processedEvents;
    private List<CDCEventStats> eventStats;
    private List<CDCEventLag> lagInfo;
    private List<ReplicationHealth> replicationHealth;
    private Instant lastUpdated;

    public double getProcessingRate() {
        if (totalEvents == null || totalEvents == 0) return 0.0;
        return (double) processedEvents / totalEvents * 100.0;
    }

    public boolean isHealthy() {
        return replicationHealth != null &&
               replicationHealth.stream()
                   .allMatch(h -> "HEALTHY".equals(h.getStatus()));
    }
}
