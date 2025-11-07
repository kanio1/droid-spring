package com.droid.bss.domain.capacity;

import lombok.Builder;
import lombok.Data;
import com.droid.bss.domain.timeseries.CapacityPlanningData;
import com.droid.bss.domain.capacity.CapacityTrend;
import com.droid.bss.domain.capacity.CapacityForecast;
import com.droid.bss.domain.capacity.CapacityRecommendation;

import java.time.Instant;
import java.util.List;

/**
 * Comprehensive capacity planning analysis
 */
@Data
@Builder
public class CapacityPlanningAnalysis {

    private String resourceType;
    private CapacityPlanningData currentData;
    private CapacityPlanningData previousData;
    private List<CapacityTrend> trends;
    private CapacityForecast forecast;
    private List<CapacityRecommendation> recommendations;
    private Instant analysisTimestamp;

    public String getSummary() {
        return String.format("%s: Current Max %.1f%%, Forecast Max %.1f%%, %d recommendations",
                resourceType.toUpperCase(),
                currentData.getMaximumUsage() != null ? currentData.getMaximumUsage() : 0,
                getForecastMax(),
                recommendations.size());
    }

    private double getForecastMax() {
        if (forecast == null || forecast.getForecastPoints() == null) return 0.0;
        return forecast.getForecastPoints().stream()
                .mapToDouble(p -> p.getPredictedValue())
                .max()
                .orElse(0.0);
    }
}
