package com.droid.bss.domain.job;

import lombok.Builder;
import lombok.Data;

/**
 * Job Statistics
 * Provides summary statistics for background job processing
 */
@Data
@Builder
public class JobStatistics {

    private long totalJobs;
    private long activeJobs;
    private long cancelledJobs;
    private long totalRuns;
    private long successfulRuns;
    private long failedRuns;
    private double successRate;

    public double getSuccessRate() {
        return Math.round(successRate * 100.0) / 100.0;
    }

    public String getSuccessRateFormatted() {
        return String.format("%.2f%%", getSuccessRate());
    }

    public String getSummary() {
        return String.format("Jobs: %d active, %d cancelled | Runs: %d total, %.2f%% success rate",
                activeJobs, cancelledJobs, totalRuns, getSuccessRate());
    }
}
