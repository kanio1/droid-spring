package com.droid.bss.domain.capacity;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * Anomaly detection result
 */
@Data
@Builder
public class AnomalyDetection {

    private Instant timestamp;
    private double value;
    private double expectedValue;
    private double deviation; // Z-score or standard deviations
    private String severity; // HIGH, MEDIUM, LOW

    public boolean isHighSeverity() {
        return "HIGH".equals(severity);
    }
}
