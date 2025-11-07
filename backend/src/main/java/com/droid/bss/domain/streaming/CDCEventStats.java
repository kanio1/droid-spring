package com.droid.bss.domain.streaming;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * CDC Event Statistics
 */
@Data
@Builder
public class CDCEventStats {

    private String tableName;
    private String eventType;
    private Long eventCount;
    private Long ProcessedCount;
    private Long pendingCount;
    private Instant firstEvent;
    private Instant lastEvent;

    public double getProcessingRate() {
        if (eventCount == null || eventCount == 0) return 0.0;
        return (double) ProcessedCount / eventCount * 100.0;
    }
}
