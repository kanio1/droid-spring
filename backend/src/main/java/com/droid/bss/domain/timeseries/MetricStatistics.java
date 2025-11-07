package com.droid.bss.domain.timeseries;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * Statistical summary of a metric over a time period
 */
@Data
@Builder
public class MetricStatistics {

    private String metricName;
    private Double average;
    private Double minimum;
    private Double maximum;
    private Double standardDeviation;
    private Long sampleCount;
    private Instant startTime;
    private Instant endTime;

    public String getSummary() {
        return String.format("%s: avg=%.2f, min=%.2f, max=%.2f, samples=%d",
                metricName, average, minimum, maximum, sampleCount);
    }
}
