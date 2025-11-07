package com.droid.bss.domain.capacity;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * Capacity trend analysis
 */
@Data
@Builder
public class CapacityTrend {

    private String metricName;
    private double growthRate; // Percentage growth
    private double volatility;
    private String direction; // GROWING, DECLINING, STABLE
    private double confidence; // 0-100
    private Instant startTime;
    private Instant endTime;

    public boolean isGrowing() {
        return "GROWING".equals(direction);
    }

    public boolean isDeclining() {
        return "DECLINING".equals(direction);
    }

    public boolean isStable() {
        return "STABLE".equals(direction);
    }

    public String getTrendDescription() {
        return String.format("%s at %.2f%% (confidence: %.0f%%)", direction, growthRate, confidence);
    }
}
