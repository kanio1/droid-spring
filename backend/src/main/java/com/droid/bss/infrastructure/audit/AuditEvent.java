package com.droid.bss.infrastructure.audit;

import com.droid.bss.domain.audit.AuditAction;
import lombok.*;

import java.util.Map;

/**
 * Audit Event
 *
 * Data class for creating audit log entries
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class AuditEvent {

    private String userId;
    private String username;
    private AuditAction action;
    private String entityType;
    private String entityId;
    private String description;
    private Map<String, Object> oldValues;
    private Map<String, Object> newValues;
    private Map<String, Object> metadata;
    private String ipAddress;
    private String userAgent;
    private String sessionId;
    private String requestId;
    private String correlationId;
    private String source;
    private String version;

    @Setter
    @Getter
    private boolean success = true;

    @Getter
    @Setter
    private String errorMessage;

    @Getter
    @Setter
    private Long executionTimeMs;

    /**
     * Create a new AuditEvent
     */
    public static AuditEvent of(AuditAction action) {
        return AuditEvent.builder()
                .action(action)
                .source("BSS-System")
                .build();
    }

    /**
     * Create a new AuditEvent with entity information
     */
    public static AuditEvent of(AuditAction action, String entityType, String entityId) {
        return AuditEvent.builder()
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .source("BSS-System")
                .build();
    }
}
