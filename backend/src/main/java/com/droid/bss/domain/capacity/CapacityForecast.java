package com.droid.bss.domain.capacity;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

/**
 * Capacity forecasting data
 */
@Data
@Builder
public class CapacityForecast {

    private String resourceType;
    private List<ForecastPoint> forecastPoints;
    private double baseValue;
    private double averageGrowthRate;
    private int forecastHorizonDays;

    public double getMaximumForecastedValue() {
        if (forecastPoints == null || forecastPoints.isEmpty()) return 0.0;
        return forecastPoints.stream()
                .mapToDouble(ForecastPoint::getPredictedValue)
                .max()
                .orElse(0.0);
    }

    public double getMinimumForecastedValue() {
        if (forecastPoints == null || forecastPoints.isEmpty()) return 0.0;
        return forecastPoints.stream()
                .mapToDouble(ForecastPoint::getPredictedValue)
                .min()
                .orElse(0.0);
    }
}
