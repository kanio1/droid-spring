package com.droid.bss.domain.timeseries;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * Metric trend analysis comparing two time periods
 */
@Data
@Builder
public class MetricTrend {

    private String metricName;
    private Double currentValue;
    private Double previousValue;
    private Double changePercent;
    private String changeDirection; // INCREASE, DECREASE, NO_CHANGE
    private Instant periodStart;
    private Instant periodEnd;

    public boolean isIncreasing() {
        return "INCREASE".equals(changeDirection);
    }

    public boolean isDecreasing() {
        return "DECREASE".equals(changeDirection);
    }

    public String getChangeDescription() {
        if (currentValue == null || previousValue == null) {
            return "N/A";
        }
        return String.format("%.2f%% %s", Math.abs(changePercent), changeDirection);
    }
}
