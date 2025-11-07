package com.droid.bss.domain.capacity;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * Single forecast data point
 */
@Data
@Builder
public class ForecastPoint {

    private Instant timestamp;
    private double predictedValue;
    private double confidenceInterval; // Percentage (e.g., 95.0 for 95% confidence)
}
