package com.droid.bss.domain.streaming;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

/**
 * CDC Event representing a database change
 */
@Data
@Builder
public class CDCEvent {

    private UUID eventId;
    private Instant eventTime;
    private String eventType; // INSERT, UPDATE, DELETE
    private String tableName;
    private Long operationId;
    private UUID pkValue;
    private String oldData; // JSON
    private String newData; // JSON
    private String eventTopic;

    public boolean isInsert() {
        return "INSERT".equals(eventType);
    }

    public boolean isUpdate() {
        return "UPDATE".equals(eventType);
    }

    public boolean isDelete() {
        return "DELETE".equals(eventType);
    }
}
