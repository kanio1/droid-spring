package com.droid.bss.application.service;

import com.droid.bss.domain.timeseries.*;
import com.droid.bss.infrastructure.timeseries.TimeSeriesQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Capacity Planning Service
 * Analyzes trends and provides capacity planning recommendations
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CapacityPlanningService {

    private final TimeSeriesQueryService timeSeriesService;

    /**
     * Get comprehensive capacity planning analysis
     */
    public CapacityPlanningAnalysis getCapacityPlanningAnalysis(String resourceType) {
        log.info("Generating capacity planning analysis for: {}", resourceType);

        Instant now = Instant.now();
        Instant startTime = now.minus(30, java.time.temporal.ChronoUnit.DAYS);
        Instant previousStartTime = startTime.minus(30, java.time.temporal.ChronoUnit.DAYS);

        // Get historical data
        CapacityPlanningData currentData = timeSeriesService.getCapacityPlanningData(
                resourceType, startTime, now);
        CapacityPlanningData previousData = timeSeriesService.getCapacityPlanningData(
                resourceType, previousStartTime, startTime);

        // Calculate trends
        List<CapacityTrend> trends = calculateTrends(resourceType, startTime, now);

        // Generate forecast
        CapacityForecast forecast = generateForecast(resourceType, startTime, now, trends);

        // Get recommendations
        List<CapacityRecommendation> recommendations = generateRecommendations(
                currentData, trends, forecast);

        return CapacityPlanningAnalysis.builder()
                .resourceType(resourceType)
                .currentData(currentData)
                .previousData(previousData)
                .trends(trends)
                .forecast(forecast)
                .recommendations(recommendations)
                .analysisTimestamp(now)
                .build();
    }

    /**
     * Calculate trends for multiple resource types
     */
    public Map<String, CapacityPlanningAnalysis> getAllCapacityAnalysis() {
        log.info("Generating capacity analysis for all resource types");

        List<String> resourceTypes = Arrays.asList("cpu", "memory", "disk", "network");

        return resourceTypes.stream()
                .collect(Collectors.toMap(
                        resourceType -> resourceType,
                        this::getCapacityPlanningAnalysis
                ));
    }

    /**
     * Get resource growth forecast
     */
    public ResourceGrowthForecast getResourceGrowthForecast(String resourceType,
                                                            int forecastDays) {
        log.info("Generating resource growth forecast for: {} days ahead", forecastDays);

        Instant now = Instant.now();
        Instant startTime = now.minus(90, java.time.temporal.ChronoUnit.DAYS); // Last 90 days for trend
        Instant endTime = now;

        // Get historical data points
        List<TimeSeriesDataPoint> historicalData = timeSeriesService.getMetricData(
                "resource_" + resourceType, startTime, endTime, "1d");

        // Calculate growth rate
        double growthRate = calculateGrowthRate(historicalData);

        // Project future values
        List<ForecastPoint> forecastPoints = projectFutureValues(
                historicalData, growthRate, forecastDays);

        // Calculate confidence interval
        double confidence = calculateConfidence(historicalData);

        return ResourceGrowthForecast.builder()
                .resourceType(resourceType)
                .historicalData(historicalData)
                .forecastPoints(forecastPoints)
                .growthRate(growthRate)
                .forecastDays(forecastDays)
                .confidence(confidence)
                .build();
    }

    /**
     * Detect anomalies in resource usage
     */
    public List<AnomalyDetection> detectAnomalies(String resourceType) {
        log.info("Detecting anomalies for resource type: {}", resourceType);

        Instant now = Instant.now();
        Instant startTime = now.minus(7, java.time.temporal.ChronoUnit.DAYS);

        List<TimeSeriesDataPoint> data = timeSeriesService.getMetricData(
                "resource_" + resourceType, startTime, now, "1h");

        return detectAnomaliesInData(data);
    }

    /**
     * Calculate capacity needs based on growth trends
     */
    public CapacityRequirement calculateCapacityRequirement(String resourceType,
                                                           int planningHorizonDays,
                                                           double safetyMarginPercent) {
        log.info("Calculating capacity requirement for {} with {} day horizon",
                resourceType, planningHorizonDays);

        Instant now = Instant.now();
        Instant startTime = now.minus(90, java.time.temporal.ChronoUnit.DAYS);

        // Get current and historical usage
        CapacityPlanningData currentData = timeSeriesService.getCapacityPlanningData(
                resourceType, startTime, now);

        // Calculate growth trend
        double dailyGrowthRate = calculateDailyGrowthRate(
                timeSeriesService.getMetricData("resource_" + resourceType, startTime, now, "1d"));

        // Project peak usage
        double projectedPeakUsage = currentData.getMaximumUsage() *
                Math.pow(1 + dailyGrowthRate / 100, planningHorizonDays);

        // Apply safety margin
        double requiredCapacity = projectedPeakUsage * (1 + safetyMarginPercent / 100);

        // Calculate when current capacity will be exceeded
        Instant exceedDate = calculateExceedDate(
                currentData.getMaximumUsage(), dailyGrowthRate, safetyMarginPercent);

        return CapacityRequirement.builder()
                .resourceType(resourceType)
                .currentUsage(currentData.getMaximumUsage())
                .projectedPeakUsage(projectedPeakUsage)
                .requiredCapacity(requiredCapacity)
                .safetyMargin(safetyMarginPercent)
                .planningHorizonDays(planningHorizonDays)
                .dailyGrowthRate(dailyGrowthRate)
                .capacityExceedDate(exceedDate)
                .build();
    }

    /**
     * Generate optimization recommendations
     */
    public List<OptimizationRecommendation> getOptimizationRecommendations(String resourceType) {
        log.info("Generating optimization recommendations for: {}", resourceType);

        List<OptimizationRecommendation> recommendations = new ArrayList<>();

        Instant now = Instant.now();
        Instant startTime = now.minus(30, java.time.temporal.ChronoUnit.DAYS);

        CapacityPlanningData data = timeSeriesService.getCapacityPlanningData(
                resourceType, startTime, now);

        // Under-utilization recommendation
        if (data.getAverageUsage() < 30) {
            recommendations.add(OptimizationRecommendation.builder()
                    .type("UNDER_UTILIZATION")
                    .severity("MEDIUM")
                    .title("Resource Under-Utilization Detected")
                    .description(String.format("Average usage is only %.1f%%. Consider right-sizing.",
                            data.getAverageUsage()))
                    .suggestedAction("Downsize or consolidate resources")
                    .estimatedSavings(getEstimatedSavings(data))
                    .build());
        }

        // High utilization recommendation
        if (data.getMaximumUsage() > 85) {
            recommendations.add(OptimizationRecommendation.builder()
                    .type("HIGH_UTILIZATION")
                    .severity("HIGH")
                    .title("High Resource Utilization")
                    .description(String.format("Peak usage reached %.1f%%. Risk of performance degradation.",
                            data.getMaximumUsage()))
                    .suggestedAction("Scale up or optimize resource usage")
                    .priority("IMMEDIATE")
                    .build());
        }

        // Growth trend recommendation
        double growthRate = calculateDailyGrowthRate(
                timeSeriesService.getMetricData("resource_" + resourceType, startTime, now, "1d"));

        if (growthRate > 2.0) {
            recommendations.add(OptimizationRecommendation.builder()
                    .type("GROWTH_TREND")
                    .severity("MEDIUM")
                    .title("Rapid Growth Detected")
                    .description(String.format("Resource usage growing at %.2f%% per day", growthRate))
                    .suggestedAction("Plan for capacity expansion")
                    .priority("WITHIN_30_DAYS")
                    .build());
        }

        return recommendations;
    }

    /**
     * Private helper methods
     */
    private List<CapacityTrend> calculateTrends(String resourceType,
                                                Instant startTime,
                                                Instant endTime) {
        List<CapacityTrend> trends = new ArrayList<>();

        // Calculate daily trends
        List<TimeSeriesDataPoint> dailyData = timeSeriesService.getMetricData(
                "resource_" + resourceType, startTime, endTime, "1d");

        double averageGrowth = calculateGrowthRate(dailyData);
        double volatility = calculateVolatility(dailyData);
        double trendDirection = averageGrowth > 0 ? 1 : averageGrowth < 0 ? -1 : 0;

        trends.add(CapacityTrend.builder()
                .metricName(resourceType)
                .growthRate(averageGrowth)
                .volatility(volatility)
                .direction(trendDirection > 0 ? "GROWING" : trendDirection < 0 ? "DECLINING" : "STABLE")
                .confidence(calculateConfidence(dailyData))
                .startTime(startTime)
                .endTime(endTime)
                .build());

        return trends;
    }

    private CapacityForecast generateForecast(String resourceType,
                                             Instant startTime,
                                             Instant endTime,
                                             List<CapacityTrend> trends) {
        double baseValue = timeSeriesService.getLatestMetricValue("resource_" + resourceType) != null
                ? timeSeriesService.getLatestMetricValue("resource_" + resourceType)
                : 0.0;

        CapacityTrend mainTrend = trends.isEmpty() ? null : trends.get(0);
        double growthRate = mainTrend != null ? mainTrend.getGrowthRate() : 0.0;

        List<ForecastPoint> points = new ArrayList<>();
        Instant forecastStart = endTime;

        for (int i = 1; i <= 30; i++) { // 30 days forecast
            Instant forecastTime = forecastStart.plus(i, java.time.temporal.ChronoUnit.DAYS);
            double forecastValue = baseValue * Math.pow(1 + growthRate / 100, i);

            points.add(ForecastPoint.builder()
                    .timestamp(forecastTime)
                    .predictedValue(forecastValue)
                    .confidenceInterval(calculateConfidenceInterval(forecastValue, mainTrend))
                    .build());
        }

        return CapacityForecast.builder()
                .resourceType(resourceType)
                .forecastPoints(points)
                .baseValue(baseValue)
                .averageGrowthRate(growthRate)
                .forecastHorizonDays(30)
                .build();
    }

    private List<CapacityRecommendation> generateRecommendations(CapacityPlanningData currentData,
                                                                 List<CapacityTrend> trends,
                                                                 CapacityForecast forecast) {
        List<CapacityRecommendation> recommendations = new ArrayList<>();

        double maxForecast = forecast.getForecastPoints().stream()
                .mapToDouble(ForecastPoint::getPredictedValue)
                .max()
                .orElse(0.0);

        if (maxForecast > 90) {
            recommendations.add(CapacityRecommendation.builder()
                    .type("SCALE_UP")
                    .priority("CRITICAL")
                    .title("Immediate Scaling Required")
                    .description("Projected usage will exceed 90% of capacity")
                    .suggestedCapacity(maxForecast * 1.5) // 50% headroom
                    .timeframe("IMMEDIATE")
                    .build());
        } else if (maxForecast > 75) {
            recommendations.add(CapacityRecommendation.builder()
                    .type("PLAN_SCALE_UP")
                    .priority("HIGH")
                    .title("Plan Capacity Increase")
                    .description("Projected usage will approach 75% of capacity")
                    .suggestedCapacity(maxForecast * 1.3) // 30% headroom
                    .timeframe("WITHIN_30_DAYS")
                    .build());
        }

        return recommendations;
    }

    private double calculateGrowthRate(List<TimeSeriesDataPoint> data) {
        if (data.size() < 2) return 0.0;

        double firstValue = data.get(0).getAverageValue();
        double lastValue = data.get(data.size() - 1).getAverageValue();

        if (firstValue == 0) return 0.0;

        return ((lastValue - firstValue) / firstValue) * 100;
    }

    private double calculateDailyGrowthRate(List<TimeSeriesDataPoint> data) {
        if (data.size() < 2) return 0.0;

        // Calculate average daily change
        double totalChange = 0.0;
        for (int i = 1; i < data.size(); i++) {
            double prevValue = data.get(i - 1).getAverageValue();
            double currentValue = data.get(i).getAverageValue();
            if (prevValue > 0) {
                totalChange += (currentValue - prevValue) / prevValue;
            }
        }

        return (totalChange / (data.size() - 1)) * 100;
    }

    private double calculateVolatility(List<TimeSeriesDataPoint> data) {
        if (data.size() < 2) return 0.0;

        double mean = data.stream().mapToDouble(TimeSeriesDataPoint::getAverageValue).average().orElse(0.0);
        double variance = data.stream()
                .mapToDouble(d -> Math.pow(d.getAverageValue() - mean, 2))
                .average()
                .orElse(0.0);

        return Math.sqrt(variance);
    }

    private double calculateConfidence(List<TimeSeriesDataPoint> data) {
        if (data.size() < 2) return 50.0;

        double volatility = calculateVolatility(data);
        double mean = data.stream().mapToDouble(TimeSeriesDataPoint::getAverageValue).average().orElse(0.0);

        if (mean == 0) return 50.0;

        double coefficientOfVariation = volatility / mean;
        return Math.max(0, Math.min(100, 100 - (coefficientOfVariation * 100)));
    }

    private List<ForecastPoint> projectFutureValues(List<TimeSeriesDataPoint> historicalData,
                                                    double growthRate,
                                                    int forecastDays) {
        List<ForecastPoint> forecast = new ArrayList<>();
        Instant lastTime = historicalData.isEmpty()
                ? Instant.now()
                : historicalData.get(historicalData.size() - 1).getTimestamp();

        for (int i = 1; i <= forecastDays; i++) {
            Instant forecastTime = lastTime.plus(i, java.time.temporal.ChronoUnit.DAYS);
            double baseValue = historicalData.get(historicalData.size() - 1).getAverageValue();
            double forecastValue = baseValue * Math.pow(1 + growthRate / 100, i);

            forecast.add(ForecastPoint.builder()
                    .timestamp(forecastTime)
                    .predictedValue(forecastValue)
                    .confidenceInterval(95.0)
                    .build());
        }

        return forecast;
    }

    private List<AnomalyDetection> detectAnomaliesInData(List<TimeSeriesDataPoint> data) {
        List<AnomalyDetection> anomalies = new ArrayList<>();

        if (data.size() < 5) return anomalies;

        double mean = data.stream().mapToDouble(TimeSeriesDataPoint::getAverageValue).average().orElse(0.0);
        double stdDev = Math.sqrt(data.stream()
                .mapToDouble(d -> Math.pow(d.getAverageValue() - mean, 2))
                .average()
                .orElse(0.0));

        double threshold = 2.0; // 2 standard deviations

        for (TimeSeriesDataPoint point : data) {
            double zScore = stdDev > 0 ? Math.abs(point.getAverageValue() - mean) / stdDev : 0;

            if (zScore > threshold) {
                anomalies.add(AnomalyDetection.builder()
                        .timestamp(point.getTimestamp())
                        .value(point.getAverageValue())
                        .expectedValue(mean)
                        .deviation(zScore)
                        .severity(zScore > 3 ? "HIGH" : "MEDIUM")
                        .build());
            }
        }

        return anomalies;
    }

    private double calculateConfidenceInterval(double value, CapacityTrend trend) {
        if (trend == null) return 95.0;
        return Math.max(50.0, Math.min(99.0, trend.getConfidence()));
    }

    private Instant calculateExceedDate(double currentUsage,
                                       double dailyGrowthRate,
                                       double safetyMargin) {
        if (dailyGrowthRate <= 0) return null;

        double targetUsage = 80.0 + safetyMargin; // 80% threshold

        if (currentUsage >= targetUsage) return Instant.now();

        double daysToExceed = Math.log(targetUsage / currentUsage) / Math.log(1 + dailyGrowthRate / 100);

        return Instant.now().plus((long) daysToExceed, java.time.temporal.ChronoUnit.DAYS);
    }

    private double getEstimatedSavings(CapacityPlanningData data) {
        if (data.getAverageUsage() < 30) {
            return (30 - data.getAverageUsage()) * 100; // Simplified calculation
        }
        return 0.0;
    }
}
