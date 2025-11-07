package com.droid.bss.domain.job;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Background Job Entity
 * Represents a scheduled background task
 */
@Entity
@Table(name = "background_jobs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BackgroundJob {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "cron_expression", nullable = false)
    private String cronExpression;

    @Column(name = "sql_command", nullable = false, columnDefinition = "TEXT")
    private String sqlCommand;

    @Column(name = "status", nullable = false)
    private String status; // ACTIVE, CANCELLED, FAILED

    @Column(name = "priority")
    private String priority; // HIGH, MEDIUM, LOW

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "last_run_at")
    private Instant lastRunAt;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "retry_count")
    private Integer retryCount;

    @Column(name = "max_retries")
    private Integer maxRetries;

    public boolean isActive() {
        return "ACTIVE".equals(status);
    }

    public boolean hasExceededMaxRetries() {
        return retryCount != null && maxRetries != null && retryCount >= maxRetries;
    }

    public String getSummary() {
        return String.format("Job: %s (Status: %s, Priority: %s)", name, status, priority);
    }
}
