package com.droid.bss.domain.timeseries;

import lombok.Builder;
import lombok.Data;

/**
 * Summary of a metric across all data points
 */
@Data
@Builder
public class MetricSummary {

    private String metricName;
    private Double averageValue;
    private Double maximumValue;
    private Long sampleCount;

    public String getDisplayName() {
        return metricName.replace("_", " ").toUpperCase();
    }
}
