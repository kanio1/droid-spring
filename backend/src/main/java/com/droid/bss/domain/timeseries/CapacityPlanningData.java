package com.droid.bss.domain.timeseries;

import lombok.Builder;
import lombok.Data;

/**
 * Capacity planning analysis data
 */
@Data
@Builder
public class CapacityPlanningData {

    private String resourceType;
    private Double averageUsage;
    private Double maximumUsage;
    private Double minimumUsage;
    private String capacityRecommendation;

    public String getStatusColor() {
        if (maximumUsage == null) return "gray";
        if (maximumUsage > 90) return "red";
        if (maximumUsage > 80) return "orange";
        if (maximumUsage > 70) return "yellow";
        return "green";
    }
}
