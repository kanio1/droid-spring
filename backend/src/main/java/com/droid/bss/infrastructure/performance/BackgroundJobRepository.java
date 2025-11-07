package com.droid.bss.infrastructure.performance;

import com.droid.bss.domain.job.BackgroundJob;
import com.droid.bss.domain.job.BackgroundJobRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for background job management
 */
@Repository
public interface BackgroundJobRepository extends JpaRepository<BackgroundJob, UUID> {

    /**
     * Find all active jobs
     */
    @Query("SELECT j FROM BackgroundJob j WHERE j.status = 'ACTIVE' ORDER BY j.createdAt DESC")
    List<BackgroundJob> findActiveJobs();

    /**
     * Get job runs for a specific job
     */
    @Query("SELECT r FROM BackgroundJobRun r WHERE r.jobId = :jobId ORDER BY r.startedAt DESC")
    List<BackgroundJobRun> getJobRuns(@Param("jobId") UUID jobId, @Param("limit") int limit);

    /**
     * Get recent job runs
     */
    @Query("SELECT r FROM BackgroundJobRun r ORDER BY r.startedAt DESC")
    List<BackgroundJobRun> getRecentRuns(@Param("limit") int limit);

    /**
     * Save job run
     */
    @Modifying
    @Query("INSERT INTO BackgroundJobRun r (r.id, r.jobId, r.status, r.startedAt, r.finishedAt, r.executionTimeMs, r.errorMessage, r.retryCount) " +
           "VALUES (:id, :jobId, :status, :startedAt, :finishedAt, :executionTimeMs, :errorMessage, :retryCount)")
    void saveJobRun(@Param("id") UUID id,
                    @Param("jobId") UUID jobId,
                    @Param("status") String status,
                    @Param("startedAt") Instant startedAt,
                    @Param("finishedAt") Instant finishedAt,
                    @Param("executionTimeMs") Long executionTimeMs,
                    @Param("errorMessage") String errorMessage,
                    @Param("retryCount") int retryCount);

    /**
     * Delete old job runs
     */
    @Modifying
    @Query("DELETE FROM BackgroundJobRun r WHERE r.finishedAt < :cutoffDate")
    int deleteOldRuns(@Param("cutoffDate") Instant cutoffDate);

    /**
     * Get job statistics by status
     */
    @Query("SELECT COUNT(j) FROM BackgroundJob j WHERE j.status = :status")
    long countByStatus(@Param("status") String status);

    /**
     * Find jobs by priority
     */
    @Query("SELECT j FROM BackgroundJob j WHERE j.priority = :priority ORDER BY j.createdAt DESC")
    List<BackgroundJob> findByPriority(@Param("priority") String priority);
}
