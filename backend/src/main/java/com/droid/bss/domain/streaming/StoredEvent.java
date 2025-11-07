package com.droid.bss.domain.streaming;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

/**
 * Stored event for event sourcing
 */
@Data
@Builder
public class StoredEvent {
    private UUID eventId;
    private String eventType;
    private Integer eventVersion;
    private UUID aggregateId;
    private String aggregateType;
    private String eventData; // JSON
    private String metadata; // JSON
    private Instant eventTime;
    private Integer processedSequence;
}
