package com.droid.bss.api.job;

import com.droid.bss.domain.job.BackgroundJob;
import com.droid.bss.domain.job.BackgroundJobRun;
import com.droid.bss.domain.job.JobStatistics;
import com.droid.bss.infrastructure.performance.JobSchedulerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Background Job Management API
 * Provides endpoints for scheduling, monitoring, and managing background jobs
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
@Tag(name = "Background Jobs", description = "Background job scheduling and monitoring")
public class JobController {

    private final JobSchedulerService jobSchedulerService;

    @GetMapping
    @Operation(summary = "Get all background jobs", description = "Returns list of all background jobs with their current status")
    public ResponseEntity<List<BackgroundJob>> getAllJobs() {
        log.debug("Fetching all background jobs");
        List<BackgroundJob> jobs = jobSchedulerService.getAllJobs();
        return ResponseEntity.ok(jobs);
    }

    @GetMapping("/{jobId}")
    @Operation(summary = "Get job by ID", description = "Returns details of a specific background job")
    public ResponseEntity<BackgroundJob> getJobById(@PathVariable UUID jobId) {
        log.debug("Fetching job: {}", jobId);
        Optional<BackgroundJob> job = jobSchedulerService.getJobById(jobId);
        return job.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{jobId}/runs")
    @Operation(summary = "Get job runs", description = "Returns execution history for a specific job")
    public ResponseEntity<List<BackgroundJobRun>> getJobRuns(
            @PathVariable UUID jobId,
            @RequestParam(defaultValue = "10") int limit) {
        log.debug("Fetching runs for job: {}", jobId);
        List<BackgroundJobRun> runs = jobSchedulerService.getJobRuns(jobId, limit);
        return ResponseEntity.ok(runs);
    }

    @GetMapping("/statistics")
    @Operation(summary = "Get job statistics", description = "Returns summary statistics for all background jobs")
    public ResponseEntity<JobStatistics> getJobStatistics() {
        log.debug("Fetching job statistics");
        JobStatistics stats = jobSchedulerService.getJobStatistics();
        return ResponseEntity.ok(stats);
    }

    @PostMapping
    @Operation(summary = "Schedule a new job", description = "Creates and schedules a new background job with the specified cron expression")
    public ResponseEntity<BackgroundJob> scheduleJob(
            @RequestBody ScheduleJobRequest request) {
        log.info("Scheduling new job: {}", request.getName());

        try {
            BackgroundJob job = jobSchedulerService.scheduleJob(
                    request.getName(),
                    request.getCronExpression(),
                    request.getSqlCommand(),
                    request.getDescription(),
                    request.getPriority()
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(job);
        } catch (Exception e) {
            log.error("Failed to schedule job: {}", request.getName(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{jobId}/cancel")
    @Operation(summary = "Cancel a job", description = "Cancels a scheduled job and unschedules it from pg_cron")
    public ResponseEntity<Map<String, String>> cancelJob(@PathVariable UUID jobId) {
        log.info("Cancelling job: {}", jobId);

        try {
            jobSchedulerService.cancelJob(jobId);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Job cancelled successfully",
                    "jobId", jobId.toString()
            ));
        } catch (Exception e) {
            log.error("Failed to cancel job: {}", jobId, e);
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Failed to cancel job: " + e.getMessage()
            ));
        }
    }

    @PutMapping("/{jobId}/schedule")
    @Operation(summary = "Update job schedule", description = "Updates the cron expression for an existing job")
    public ResponseEntity<BackgroundJob> updateJobSchedule(
            @PathVariable UUID jobId,
            @RequestBody UpdateScheduleRequest request) {
        log.info("Updating job schedule: {} to: {}", jobId, request.getCronExpression());

        try {
            BackgroundJob job = jobSchedulerService.updateJobSchedule(jobId, request.getCronExpression());
            return ResponseEntity.ok(job);
        } catch (Exception e) {
            log.error("Failed to update job schedule: {}", jobId, e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/cleanup")
    @Operation(summary = "Clean up old job runs", description = "Deletes job run history older than the specified number of days")
    public ResponseEntity<Map<String, String>> cleanupOldRuns(
            @RequestParam(defaultValue = "30") int daysToKeep) {
        log.info("Cleaning up job runs older than {} days", daysToKeep);

        try {
            jobSchedulerService.cleanupOldJobRuns(daysToKeep);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Old job runs cleaned up successfully",
                    "daysToKeep", String.valueOf(daysToKeep)
            ));
        } catch (Exception e) {
            log.error("Failed to cleanup old job runs", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Failed to cleanup: " + e.getMessage()
            ));
        }
    }

    /**
     * Request DTO for scheduling a new job
     */
    public static class ScheduleJobRequest {
        private String name;
        private String description;
        private String cronExpression;
        private String sqlCommand;
        private String priority;

        // Getters and setters
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getCronExpression() {
            return cronExpression;
        }

        public void setCronExpression(String cronExpression) {
            this.cronExpression = cronExpression;
        }

        public String getSqlCommand() {
            return sqlCommand;
        }

        public void setSqlCommand(String sqlCommand) {
            this.sqlCommand = sqlCommand;
        }

        public String getPriority() {
            return priority;
        }

        public void setPriority(String priority) {
            this.priority = priority;
        }
    }

    /**
     * Request DTO for updating job schedule
     */
    public static class UpdateScheduleRequest {
        private String cronExpression;

        public String getCronExpression() {
            return cronExpression;
        }

        public void setCronExpression(String cronExpression) {
            this.cronExpression = cronExpression;
        }
    }
}
