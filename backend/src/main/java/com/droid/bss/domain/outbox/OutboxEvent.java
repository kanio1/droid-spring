package com.droid.bss.domain.outbox;

import com.droid.bss.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Where;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Outbox Event Entity
 *
 * Implements the Outbox Pattern for reliable event publishing
 * Ensures events are not lost even if the message broker is unavailable
 */
@Entity
@Table(name = "outbox_event", indexes = {
    @Index(name = "idx_outbox_status", columnList = "status"),
    @Index(name = "idx_outbox_type", columnList = "event_type"),
    @Index(name = "idx_outbox_aggregate", columnList = "aggregate_id"),
    @Index(name = "idx_outbox_created", columnList = "created_at"),
    @Index(name = "idx_outbox_status_type", columnList = "status,event_type"),
    @Index(name = "idx_outbox_retry", columnList = "status,next_retry_at,retry_count"),
    @Index(name = "idx_outbox_trace", columnList = "trace_id"),
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_outbox_event_id", columnNames = {"event_id"})
})
@Where(clause = "deleted_at IS NULL")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class OutboxEvent extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "event_id", nullable = false, unique = true, length = 36)
    private String eventId;

    @Column(name = "event_type", nullable = false, length = 100)
    @Enumerated(EnumType.STRING)
    private OutboxEventType eventType;

    @Column(name = "event_name", nullable = false, length = 100)
    private String eventName;

    @Column(name = "aggregate_id", length = 255)
    private String aggregateId;

    @Column(name = "aggregate_type", length = 100)
    private String aggregateType;

    @Column(name = "event_data", columnDefinition = "JSONB", nullable = false)
    private String eventData;

    @Column(name = "metadata", columnDefinition = "JSONB")
    private String metadata;

    @Column(name = "version", length = 50)
    private String version;

    @Column(name = "source", length = 255)
    private String source;

    @Column(name = "correlation_id", length = 255)
    private String correlationId;

    @Column(name = "causation_id", length = 255)
    private String causationId;

    @Column(name = "user_id", length = 255)
    private String userId;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "status", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private OutboxStatus status;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount;

    @Column(name = "max_retries", nullable = false)
    private Integer maxRetries;

    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "trace_id", length = 255)
    private String traceId;

    /**
     * Check if event is ready for publishing
     */
    public boolean isPending() {
        return status == OutboxStatus.PENDING;
    }

    /**
     * Check if event has exceeded max retries
     */
    public boolean hasExceededMaxRetries() {
        return retryCount >= maxRetries;
    }

    /**
     * Check if event is dead letter
     */
    public boolean isDeadLetter() {
        return status == OutboxStatus.DEAD_LETTER;
    }

    /**
     * Mark event as published
     */
    public void markAsPublished() {
        this.status = OutboxStatus.PUBLISHED;
        this.publishedAt = LocalDateTime.now();
        this.errorMessage = null;
    }

    /**
     * Mark event as failed
     */
    public void markAsFailed(String errorMessage) {
        this.retryCount++;
        this.status = hasExceededMaxRetries() ? OutboxStatus.DEAD_LETTER : OutboxStatus.RETRY;
        this.errorMessage = errorMessage;
        this.nextRetryAt = calculateNextRetry();
    }

    /**
     * Calculate next retry time (exponential backoff)
     */
    private LocalDateTime calculateNextRetry() {
        if (hasExceededMaxRetries()) {
            return null;
        }
        // Exponential backoff: 2^retry_count seconds, max 1 hour
        long delaySeconds = Math.min((long) Math.pow(2, retryCount), 3600);
        return LocalDateTime.now().plusSeconds(delaySeconds);
    }

    /**
     * Get JSON data as typed object
     */
    public <T> T getEventDataAs(Class<T> clazz) {
        try {
            return com.fasterxml.jackson.databind.ObjectMapper().readValue(eventData, clazz);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize event data", e);
        }
    }

    /**
     * Get metadata as typed object
     */
    public <T> T getMetadataAs(Class<T> clazz) {
        if (metadata == null) {
            return null;
        }
        try {
            return com.fasterxml.jackson.databind.ObjectMapper().readValue(metadata, clazz);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize metadata", e);
        }
    }
}
