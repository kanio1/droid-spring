package com.droid.bss.domain.security;

import lombok.Builder;
import lombok.Data;

/**
 * Security metrics summary
 */
@Data
@Builder
public class SecurityMetrics {

    private Long encryptedColumns;
    private Long totalClassifiedColumns;
    private Double encryptionCoveragePercent;
    private Long eventsLast24h;
    private Long eventsLast7d;
    private Long failedEvents24h;
    private Long rlsEnabledTables;
    private Long totalTables;
    private Double rlsCoveragePercent;

    public String getSummary() {
        return String.format(
            "Encryption: %d/%d columns (%.1f%%) | RLS: %d/%d tables (%.1f%%) | Events: %d in 24h, %d failed",
            encryptedColumns, totalClassifiedColumns, encryptionCoveragePercent,
            rlsEnabledTables, totalTables, rlsCoveragePercent,
            eventsLast24h, failedEvents24h
        );
    }

    public boolean isEncryptionCompliant() {
        return encryptionCoveragePercent != null && encryptionCoveragePercent >= 95.0;
    }

    public boolean isRLSCompliant() {
        return rlsCoveragePercent != null && rlsCoveragePercent >= 90.0;
    }

    public String getOverallSecurityGrade() {
        double score = 0.0;
        int checks = 0;

        if (isEncryptionCompliant()) score += 40;
        if (isRLSCompliant()) score += 40;
        if (failedEvents24h < 10) score += 20;

        if (score >= 90) return "A";
        if (score >= 80) return "B";
        if (score >= 70) return "C";
        if (score >= 60) return "D";
        return "F";
    }
}
