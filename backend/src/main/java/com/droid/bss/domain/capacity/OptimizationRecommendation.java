package com.droid.bss.domain.capacity;

import lombok.Builder;
import lombok.Data;

/**
 * Optimization recommendation
 */
@Data
@Builder
public class OptimizationRecommendation {

    private String type; // UNDER_UTILIZATION, HIGH_UTILIZATION, GROWTH_TREND
    private String severity; // CRITICAL, HIGH, MEDIUM, LOW
    private String title;
    private String description;
    private String suggestedAction;
    private String priority; // IMMEDIATE, WITHIN_7_DAYS, WITHIN_30_DAYS
    private Double estimatedSavings;

    public boolean isHighPriority() {
        return "CRITICAL".equals(severity) || "HIGH".equals(severity);
    }
}
