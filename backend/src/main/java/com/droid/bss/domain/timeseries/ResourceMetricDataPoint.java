package com.droid.bss.domain.timeseries;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * Resource metric data point (CPU, memory, disk, etc.)
 */
@Data
@Builder
public class ResourceMetricDataPoint {

    private Instant timestamp;
    private Double averageUsagePercent;
    private Double maximumUsagePercent;
    private String resourceName;
}
