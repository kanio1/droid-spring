package com.droid.bss.domain.capacity;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * Capacity requirement calculation
 */
@Data
@Builder
public class CapacityRequirement {

    private String resourceType;
    private double currentUsage;
    private double projectedPeakUsage;
    private double requiredCapacity;
    private double safetyMargin;
    private int planningHorizonDays;
    private double dailyGrowthRate;
    private Instant capacityExceedDate;

    public boolean needsImmediateAction() {
        return capacityExceedDate != null &&
                capacityExceedDate.isBefore(Instant.now().plus(30, java.time.temporal.ChronoUnit.DAYS));
    }

    public String getTimeframeDescription() {
        if (capacityExceedDate == null) return "No action needed";
        long days = java.time.Duration.between(Instant.now(), capacityExceedDate).toDays();
        return String.format("Capacity will be exceeded in %d days", days);
    }
}
