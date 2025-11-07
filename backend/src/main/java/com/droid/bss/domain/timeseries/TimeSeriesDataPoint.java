package com.droid.bss.domain.timeseries;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * Time-series data point
 * Represents a single data point in a time-series
 */
@Data
@Builder
public class TimeSeriesDataPoint {

    private Instant timestamp;
    private Double averageValue;
    private Double minimumValue;
    private Double maximumValue;
    private Long sampleCount;

    public Double getValue() {
        return averageValue != null ? averageValue : 0.0;
    }
}
