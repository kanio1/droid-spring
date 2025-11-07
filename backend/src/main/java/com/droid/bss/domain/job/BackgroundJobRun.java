package com.droid.bss.domain.job;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Background Job Run Entity
 * Represents a single execution of a background job
 */
@Entity
@Table(name = "background_job_runs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BackgroundJobRun {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "job_id", nullable = false)
    private UUID jobId;

    @Column(name = "status", nullable = false)
    private String status; // SUCCESS, FAILED, RUNNING

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "execution_time_ms")
    private Long executionTimeMs;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "retry_count")
    private Integer retryCount;

    public boolean isSuccess() {
        return "SUCCESS".equals(status);
    }

    public boolean isFailed() {
        return "FAILED".equals(status);
    }

    public String getDurationFormatted() {
        if (executionTimeMs == null) {
            return "N/A";
        }

        if (executionTimeMs < 1000) {
            return executionTimeMs + "ms";
        } else if (executionTimeMs < 60000) {
            return String.format("%.2fs", executionTimeMs / 1000.0);
        } else {
            return String.format("%.2fm", executionTimeMs / 60000.0);
        }
    }

    public String getSummary() {
        return String.format("Run: %s (Status: %s, Duration: %s)",
                jobId, status, getDurationFormatted());
    }
}
