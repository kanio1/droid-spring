package com.droid.bss.domain.audit;

import com.droid.bss.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Where;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Audit Log Entity
 *
 * Stores comprehensive audit trail of all sensitive operations
 * Implements WORM (Write Once Read Many) pattern for compliance
 */
@Entity
@Table(name = "audit_log", indexes = {
    @Index(name = "idx_audit_timestamp", columnList = "timestamp"),
    @Index(name = "idx_audit_user", columnList = "user_id"),
    @Index(name = "idx_audit_action", columnList = "action"),
    @Index(name = "idx_audit_entity", columnList = "entity_type,entity_id"),
    @Index(name = "idx_audit_ip", columnList = "ip_address"),
})
@Where(clause = "deleted_at IS NULL")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class AuditLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "timestamp", nullable = false, updatable = false)
    private LocalDateTime timestamp;

    @Column(name = "user_id", length = 255)
    private String userId;

    @Column(name = "username", length = 255)
    private String username;

    @Column(name = "action", nullable = false, length = 100)
    @Enumerated(EnumType.STRING)
    private AuditAction action;

    @Column(name = "entity_type", length = 255)
    private String entityType;

    @Column(name = "entity_id", length = 255)
    private String entityId;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "old_values", columnDefinition = "JSONB")
    @Convert(converter = JsonMapConverter.class)
    private Map<String, Object> oldValues;

    @Column(name = "new_values", columnDefinition = "JSONB")
    @Convert(converter = JsonMapConverter.class)
    private Map<String, Object> newValues;

    @Column(name = "metadata", columnDefinition = "JSONB")
    @Convert(converter = JsonMapConverter.class)
    private Map<String, Object> metadata;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(name = "session_id", length = 255)
    private String sessionId;

    @Column(name = "request_id", length = 255)
    private String requestId;

    @Column(name = "success", nullable = false)
    private Boolean success;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "execution_time_ms")
    private Long executionTimeMs;

    @Column(name = "correlation_id", length = 255)
    private String correlationId;

    @Column(name = "source", length = 255, nullable = false)
    private String source;

    @Column(name = "version", length = 50)
    private String version;

    /**
     * Check if this audit log represents a successful operation
     */
    public boolean isSuccessful() {
        return Boolean.TRUE.equals(success);
    }

    /**
     * Get duration in human-readable format
     */
    public String getExecutionTimeFormatted() {
        if (executionTimeMs == null) {
            return "N/A";
        }
        if (executionTimeMs < 1000) {
            return executionTimeMs + "ms";
        }
        return String.format("%.2fs", executionTimeMs / 1000.0);
    }
}
