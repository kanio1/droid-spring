package com.droid.bss.domain.performance;

import lombok.Builder;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Database Performance Statistics
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DatabaseStats {

    private String databaseName;
    private Long sizeBytes;
    private String sizeHuman;
    private BigDecimal cacheHitRatio;
    private Long blksRead;
    private Long blksHit;
    private Long xactCommit;
    private Long xactRollback;
    private Long blksFetch;
    private Long tupReturned;
    private Long tupFetched;
    private Long tupInserted;
    private Long tupUpdated;
    private Long tupDeleted;
    private Long conflicts;
    private Long tempFiles;
    private Long tempBytes;
    private Long deadlocks;
    private Double blkReadTime;
    private Double blkWriteTime;

    // Cache performance grade
    public String getCachePerformanceGrade() {
        if (cacheHitRatio == null) return "N/A";

        double ratio = cacheHitRatio.doubleValue();
        if (ratio >= 99) return "A";
        if (ratio >= 95) return "B";
        if (ratio >= 90) return "C";
        if (ratio >= 80) return "D";
        return "F";
    }

    // Database health score (0-100)
    public int getHealthScore() {
        if (cacheHitRatio == null) return 0;

        int score = 0;

        // Cache hit ratio contributes 50% to health score
        score += (int) (cacheHitRatio.doubleValue() * 0.5);

        // Transaction success rate contributes 30%
        long totalXact = xactCommit + xactRollback;
        if (totalXact > 0) {
            BigDecimal successRate = new BigDecimal(xactCommit)
                .divide(new BigDecimal(totalXact), 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal(30));
            score += successRate.intValue();
        }

        // Deadlocks penalty (max -20 points)
        if (deadlocks != null && deadlocks > 0) {
            int penalty = Math.min(20, (int) (deadlocks * 2));
            score = Math.max(0, score - penalty);
        }

        return Math.min(100, score);
    }

    // Performance summary
    public String getSummary() {
        return String.format("DB Size: %s | Cache Hit: %.2f%% (Grade: %s) | Health Score: %d%%",
            sizeHuman,
            cacheHitRatio != null ? cacheHitRatio.doubleValue() : 0,
            getCachePerformanceGrade(),
            getHealthScore());
    }
}
