package com.droid.bss.domain.performance;

import lombok.Builder;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Comparator;

/**
 * Table Statistics including bloat information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableStats {

    private String schemaName;
    private String tableName;
    private Long rowsInserted;
    private Long rowsUpdated;
    private Long rowsDeleted;
    private Long liveRows;
    private Long deadRows;
    private BigDecimal deadRowPercent;
    private Timestamp lastVacuum;
    private Timestamp lastAutoVacuum;
    private Timestamp lastAnalyze;
    private Timestamp lastAutoAnalyze;
    private Long vacuumCount;
    private Long autoVacuumCount;
    private Long analyzeCount;
    private Long autoAnalyzeCount;

    // Health score based on dead rows and maintenance
    public int getTableHealthScore() {
        int score = 100;

        // Dead row penalty
        if (deadRowPercent != null) {
            score -= deadRowPercent.intValue() / 2;
        }

        // Maintenance bonus
        if (lastVacuum != null || lastAutoVacuum != null) {
            score += 5;
        }

        if (lastAnalyze != null || lastAutoAnalyze != null) {
            score += 5;
        }

        return Math.max(0, Math.min(100, score));
    }

    // Maintenance recommendations
    public String getMaintenanceRecommendation() {
        if (deadRowPercent != null && deadRowPercent.compareTo(new BigDecimal(20)) > 0) {
            return "VACUUM ANALYZE needed - High dead row percentage";
        }

        if (lastVacuum == null && lastAutoVacuum == null) {
            return "Manual VACUUM recommended";
        }

        if (lastAnalyze == null && lastAutoAnalyze == null) {
            return "ANALYZE needed for optimizer statistics";
        }

        return "Table maintenance is up to date";
    }

    // Last maintenance time
    public String getLastMaintenanceTime() {
        Timestamp latest = getLatestMaintenanceTime();
        if (latest == null) return "Never";

        return latest.toString();
    }

    private Timestamp getLatestMaintenanceTime() {
        return java.util.stream.Stream.of(lastVacuum, lastAutoVacuum, lastAnalyze, lastAutoAnalyze)
            .filter(java.util.Objects::nonNull)
            .max(Comparator.naturalOrder())
            .orElse(null);
    }
}
