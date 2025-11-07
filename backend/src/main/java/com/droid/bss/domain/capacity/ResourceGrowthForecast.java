package com.droid.bss.domain.capacity;

import lombok.Builder;
import lombok.Data;
import com.droid.bss.domain.timeseries.TimeSeriesDataPoint;

import java.util.List;

/**
 * Resource growth forecast
 */
@Data
@Builder
public class ResourceGrowthForecast {

    private String resourceType;
    private List<TimeSeriesDataPoint> historicalData;
    private List<ForecastPoint> forecastPoints;
    private double growthRate; // Daily growth rate percentage
    private int forecastDays;
    private double confidence; // 0-100

    public double getProjectedValue(int daysAhead) {
        if (historicalData == null || historicalData.isEmpty()) return 0.0;

        double baseValue = historicalData.get(historicalData.size() - 1).getAverageValue();
        return baseValue * Math.pow(1 + growthRate / 100, daysAhead);
    }
}
