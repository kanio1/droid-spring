package com.droid.bss.domain.timeseries;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

/**
 * Business metric data point
 */
@Data
@Builder
public class BusinessMetricDataPoint {

    private Instant timestamp;
    private Double totalValue;
    private Double averageValue;
    private UUID customerId;
    private UUID productId;
}
