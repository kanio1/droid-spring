package com.droid.bss.infrastructure.performance;

import com.droid.bss.domain.job.BackgroundJob;
import com.droid.bss.domain.job.BackgroundJobRun;
import com.droid.bss.domain.job.BackgroundJobSchedule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Job Scheduler Service using pg_cron for background task processing
 * Manages scheduled jobs, retries, and monitoring
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JobSchedulerService {

    private final JdbcTemplate jdbcTemplate;
    private final BackgroundJobRepository jobRepository;

    /**
     * Schedule a new background job
     */
    @Transactional
    public BackgroundJob scheduleJob(String name, String cronExpression, String sqlCommand,
                                     String description, String priority) {
        log.info("Scheduling new job: {} with cron: {}", name, cronExpression);

        BackgroundJob job = BackgroundJob.builder()
                .id(UUID.randomUUID())
                .name(name)
                .cronExpression(cronExpression)
                .sqlCommand(sqlCommand)
                .description(description)
                .priority(priority != null ? priority : "MEDIUM")
                .status("ACTIVE")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .retryCount(0)
                .maxRetries(3)
                .build();

        // Store job in database
        jobRepository.save(job);

        // Schedule in pg_cron
        scheduleInPgCron(job);

        log.info("Job scheduled successfully: {} with jobId: {}", name, job.getId());
        return job;
    }

    /**
     * Schedule job in pg_cron database extension
     */
    private void scheduleInPgCron(BackgroundJob job) {
        try {
            String jobName = "job_" + job.getId().toString().replace("-", "_");
            String cronSchedule = job.getCronExpression();
            String sqlCommand = job.getSqlCommand();

            // Use pg_cron to schedule the job
            // Note: In production, you might want to use a more sophisticated approach
            // with proper error handling and transaction management
            String scheduleSql = String.format(
                "SELECT cron.schedule('%s', '%s', %s)",
                jobName, cronSchedule, sqlCommand
            );

            jdbcTemplate.queryForObject(scheduleSql, String.class);
            log.debug("Job {} scheduled in pg_cron", jobName);

        } catch (Exception e) {
            log.error("Failed to schedule job in pg_cron: {}", job.getName(), e);
            throw new RuntimeException("Failed to schedule job in pg_cron", e);
        }
    }

    /**
     * Get all background jobs
     */
    public List<BackgroundJob> getAllJobs() {
        return jobRepository.findAll();
    }

    /**
     * Get job by ID
     */
    public Optional<BackgroundJob> getJobById(UUID jobId) {
        return jobRepository.findById(jobId);
    }

    /**
     * Get job runs for a specific job
     */
    public List<BackgroundJobRun> getJobRuns(UUID jobId, int limit) {
        return jobRepository.getJobRuns(jobId, limit);
    }

    /**
     * Cancel a scheduled job
     */
    @Transactional
    public void cancelJob(UUID jobId) {
        log.info("Cancelling job: {}", jobId);

        BackgroundJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));

        // Unschedule from pg_cron
        unscheduleFromPgCron(job);

        // Update job status
        job.setStatus("CANCELLED");
        job.setUpdatedAt(Instant.now());
        jobRepository.save(job);

        log.info("Job cancelled successfully: {}", jobId);
    }

    /**
     * Unschedule job from pg_cron
     */
    private void unscheduleFromPgCron(BackgroundJob job) {
        try {
            String jobName = "job_" + job.getId().toString().replace("-", "_");
            String unscheduleSql = String.format(
                "SELECT cron.unschedule('%s')",
                jobName
            );

            jdbcTemplate.queryForObject(unscheduleSql, String.class);
            log.debug("Job {} unscheduled from pg_cron", jobName);

        } catch (Exception e) {
            log.error("Failed to unschedule job from pg_cron: {}", job.getName(), e);
            // Continue with status update even if pg_cron unschedule fails
        }
    }

    /**
     * Update job schedule
     */
    @Transactional
    public BackgroundJob updateJobSchedule(UUID jobId, String newCronExpression) {
        log.info("Updating job schedule: {} to: {}", jobId, newCronExpression);

        BackgroundJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));

        // Unschedule old job
        unscheduleFromPgCron(job);

        // Update job
        job.setCronExpression(newCronExpression);
        job.setUpdatedAt(Instant.now());

        // Reschedule with new expression
        scheduleInPgCron(job);

        jobRepository.save(job);

        log.info("Job schedule updated successfully: {}", jobId);
        return job;
    }

    /**
     * Get job statistics
     */
    public JobStatistics getJobStatistics() {
        List<BackgroundJob> allJobs = jobRepository.findAll();

        long totalJobs = allJobs.size();
        long activeJobs = allJobs.stream()
                .mapToLong(j -> "ACTIVE".equals(j.getStatus()) ? 1 : 0)
                .sum();
        long cancelledJobs = allJobs.stream()
                .mapToLong(j -> "CANCELLED".equals(j.getStatus()) ? 1 : 0)
                .sum();

        // Get recent job runs
        List<BackgroundJobRun> recentRuns = jobRepository.getRecentRuns(100);

        long successfulRuns = recentRuns.stream()
                .mapToLong(r -> "SUCCESS".equals(r.getStatus()) ? 1 : 0)
                .sum();

        long failedRuns = recentRuns.stream()
                .mapToLong(r -> "FAILED".equals(r.getStatus()) ? 1 : 0)
                .sum();

        return JobStatistics.builder()
                .totalJobs(totalJobs)
                .activeJobs(activeJobs)
                .cancelledJobs(cancelledJobs)
                .totalRuns(recentRuns.size())
                .successfulRuns(successfulRuns)
                .failedRuns(failedRuns)
                .successRate(recentRuns.isEmpty() ? 0.0 :
                        (double) successfulRuns / recentRuns.size() * 100)
                .build();
    }

    /**
     * Process job completion and handle retries
     */
    @Transactional
    public void processJobCompletion(UUID jobId, String status, String errorMessage, Long executionTimeMs) {
        BackgroundJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));

        // Record job run
        BackgroundJobRun run = BackgroundJobRun.builder()
                .id(UUID.randomUUID())
                .jobId(jobId)
                .status(status)
                .startedAt(Instant.now().minusMillis(executionTimeMs != null ? executionTimeMs : 0))
                .finishedAt(Instant.now())
                .executionTimeMs(executionTimeMs != null ? executionTimeMs : 0L)
                .errorMessage(errorMessage)
                .retryCount(job.getRetryCount())
                .build();

        jobRepository.saveJobRun(run);

        if ("FAILED".equals(status)) {
            handleJobFailure(job, errorMessage);
        } else if ("SUCCESS".equals(status)) {
            // Reset retry count on success
            job.setRetryCount(0);
            job.setLastRunAt(Instant.now());
            jobRepository.save(job);
        }
    }

    /**
     * Handle job failure and retry logic
     */
    private void handleJobFailure(BackgroundJob job, String errorMessage) {
        int currentRetries = job.getRetryCount();
        int maxRetries = job.getMaxRetries();

        if (currentRetries < maxRetries) {
            // Calculate backoff delay (exponential backoff)
            long backoffSeconds = (long) Math.pow(2, currentRetries) * 60; // 1min, 2min, 4min, etc.

            log.warn("Job {} failed (attempt {}/{}), will retry in {} seconds: {}",
                    job.getName(), currentRetries + 1, maxRetries, backoffSeconds, errorMessage);

            // Increment retry count
            job.setRetryCount(currentRetries + 1);
            job.setLastError(errorMessage);
            job.setLastRunAt(Instant.now());
            jobRepository.save(job);

            // Schedule retry with backoff
            scheduleRetry(job, backoffSeconds);

        } else {
            // Max retries exceeded
            log.error("Job {} failed after {} attempts, marking as permanently failed",
                    job.getName(), maxRetries);

            job.setStatus("FAILED");
            job.setLastError(errorMessage);
            job.setLastRunAt(Instant.now());
            jobRepository.save(job);
        }
    }

    /**
     * Schedule a retry with exponential backoff
     */
    private void scheduleRetry(BackgroundJob job, long delaySeconds) {
        try {
            String jobName = "retry_" + job.getId().toString().replace("-", "_");
            LocalDateTime nextRun = LocalDateTime.now().plusSeconds(delaySeconds);
            String cronExpression = String.format("%d %d %d %d *",
                    nextRun.getMinute(),
                    nextRun.getHour(),
                    nextRun.getDayOfMonth(),
                    nextRun.getMonthValue());

            // Schedule one-time retry job
            String scheduleSql = String.format(
                "SELECT cron.schedule('%s', '%s', %s)",
                jobName, cronExpression, job.getSqlCommand()
            );

            jdbcTemplate.queryForObject(scheduleSql, String.class);
            log.debug("Retry scheduled for job {} in {} seconds", job.getName(), delaySeconds);

        } catch (Exception e) {
            log.error("Failed to schedule retry for job: {}", job.getName(), e);
        }
    }

    /**
     * Clean up old job runs
     */
    @Transactional
    public void cleanupOldJobRuns(int daysToKeep) {
        log.info("Cleaning up job runs older than {} days", daysToKeep);

        Instant cutoffDate = Instant.now().minus(daysToKeep, java.time.temporal.ChronoUnit.DAYS);
        int deleted = jobRepository.deleteOldRuns(cutoffDate);

        log.info("Deleted {} old job runs", deleted);
    }
}
