package com.droid.bss.domain.job;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Background Job Schedule Entity
 * Represents the schedule configuration for a job
 */
@Entity
@Table(name = "background_job_schedules")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BackgroundJobSchedule {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "job_id", nullable = false)
    private UUID jobId;

    @Column(name = "cron_expression", nullable = false)
    private String cronExpression;

    @Column(name = "timezone")
    private String timezone;

    @Column(name = "next_run_at")
    private Instant nextRunAt;

    @Column(name = "last_run_at")
    private Instant lastRunAt;

    @Column(name = "is_active")
    private Boolean isActive;

    public boolean isActive() {
        return isActive != null && isActive;
    }
}
