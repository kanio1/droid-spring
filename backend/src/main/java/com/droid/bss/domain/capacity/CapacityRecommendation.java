package com.droid.bss.domain.capacity;

import lombok.Builder;
import lombok.Data;

/**
 * Capacity recommendation
 */
@Data
@Builder
public class CapacityRecommendation {

    private String type; // SCALE_UP, SCALE_DOWN, OPTIMIZE
    private String priority; // CRITICAL, HIGH, MEDIUM, LOW
    private String title;
    private String description;
    private Double suggestedCapacity;
    private String timeframe; // IMMEDIATE, WITHIN_7_DAYS, WITHIN_30_DAYS, WITHIN_90_DAYS

    public boolean isCritical() {
        return "CRITICAL".equals(priority);
    }

    public boolean isHighPriority() {
        return "CRITICAL".equals(priority) || "HIGH".equals(priority);
    }
}
