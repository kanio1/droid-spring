package com.droid.bss.application.service;

import com.droid.bss.infrastructure.timeseries.RevenueMetricsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RevenueAnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(RevenueAnalyticsService.class);

    private final RevenueMetricsRepository repository;

    public RevenueAnalyticsService(RevenueMetricsRepository repository) {
        this.repository = repository;
    }

    public void recordRevenueMetric(BigDecimal revenue, BigDecimal costs,
                                   Integer ordersCount, String region, String productCategory) {
        repository.recordRevenueMetric(revenue, costs, ordersCount, region, productCategory);
    }

    public RevenueSummary getRevenueSummary(int days) {
        List<RevenueMetricsRepository.RevenueDaily> dailyData = repository.getRevenueSummary(days);

        RevenueSummary summary = new RevenueSummary();
        summary.setDailyData(dailyData);
        summary.setTotalRevenue(calculateTotalRevenue(dailyData));
        summary.setTotalProfit(calculateTotalProfit(dailyData));
        summary.setAverageOrderValue(calculateAverageOrderValue(dailyData));
        summary.setGrowthRate(calculateGrowthRate(dailyData));
        summary.setTotalOrders(calculateTotalOrders(dailyData));

        return summary;
    }

    public List<RevenueMetricsRepository.RevenueByRegion> getRevenueByRegion(int days) {
        return repository.getRevenueByRegion(days);
    }

    public List<RevenueMetricsRepository.RevenueByCategory> getRevenueByCategory(int days) {
        return repository.getRevenueByCategory(days);
    }

    public List<RevenueMetricsRepository.GrowthRate> getGrowthRate(int days) {
        return repository.calculateGrowthRate("revenue", days);
    }

    public BigDecimal getTotalRevenue(int days) {
        Instant endTime = Instant.now();
        Instant startTime = endTime.minusSeconds(days * 24L * 60 * 60);
        return repository.getTotalRevenue(startTime, endTime);
    }

    public BigDecimal getTotalProfit(int days) {
        Instant endTime = Instant.now();
        Instant startTime = endTime.minusSeconds(days * 24L * 60 * 60);
        return repository.getTotalProfit(startTime, endTime);
    }

    public BigDecimal getAverageOrderValue(int days) {
        Instant endTime = Instant.now();
        Instant startTime = endTime.minusSeconds(days * 24L * 60 * 60);
        return repository.getAverageOrderValue(startTime, endTime);
    }

    public RevenueForecast forecastRevenue(int daysToForecast) {
        log.info("Forecasting revenue for {} days", daysToForecast);

        // Get 30 days of historical data
        int historicalDays = 30;
        List<RevenueMetricsRepository.RevenueDaily> historical = repository.getRevenueSummary(historicalDays);

        if (historical.size() < 7) {
            log.warn("Insufficient data for forecasting: only {} days available", historical.size());
            return new RevenueForecast();
        }

        // Simple linear regression for forecasting
        // In production, use libraries like Apache Spark MLlib or Python-based forecasting
        double[] revenues = historical.stream()
            .mapToDouble(d -> d.getTotalRevenue().doubleValue())
            .toArray();

        double[] days = new double[revenues.length];
        for (int i = 0; i < days.length; i++) {
            days[i] = i;
        }

        // Calculate linear regression (y = mx + b)
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
        for (int i = 0; i < revenues.length; i++) {
            sumX += days[i];
            sumY += revenues[i];
            sumXY += days[i] * revenues[i];
            sumX2 += days[i] * days[i];
        }

        double n = revenues.length;
        double slope = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX);
        double intercept = (sumY - slope * sumX) / n;

        // Generate forecast
        List<RevenueForecast.ForecastPoint> forecastPoints = new java.util.ArrayList<>();
        double lastDay = days[days.length - 1];

        for (int i = 1; i <= daysToForecast; i++) {
            double forecastDay = lastDay + i;
            double predictedRevenue = slope * forecastDay + intercept;

            RevenueForecast.ForecastPoint point = new RevenueForecast.ForecastPoint();
            point.setDay(Instant.now().plusSeconds(i * 24L * 60 * 60));
            point.setPredictedRevenue(BigDecimal.valueOf(predictedRevenue));
            point.setConfidence(calculateConfidence(i, historical.size()));

            forecastPoints.add(point);
        }

        RevenueForecast forecast = new RevenueForecast();
        forecast.setForecastPoints(forecastPoints);
        forecast.setAverageGrowthRate(slope);
        forecast.setMethod("Linear Regression");

        return forecast;
    }

    private BigDecimal calculateTotalRevenue(List<RevenueMetricsRepository.RevenueDaily> dailyData) {
        return dailyData.stream()
            .map(RevenueMetricsRepository.RevenueDaily::getTotalRevenue)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateTotalProfit(List<RevenueMetricsRepository.RevenueDaily> dailyData) {
        return dailyData.stream()
            .map(RevenueMetricsRepository.RevenueDaily::getTotalProfit)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateAverageOrderValue(List<RevenueMetricsRepository.RevenueDaily> dailyData) {
        return dailyData.stream()
            .map(RevenueMetricsRepository.RevenueDaily::getAvgOrderValue)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(BigDecimal.valueOf(dailyData.size()), 2, BigDecimal.ROUND_HALF_UP);
    }

    private Double calculateGrowthRate(List<RevenueMetricsRepository.RevenueDaily> dailyData) {
        if (dailyData.size() < 2) {
            return 0.0;
        }

        RevenueMetricsRepository.RevenueDaily latest = dailyData.get(0);
        RevenueMetricsRepository.RevenueDaily previous = dailyData.get(1);

        double latestRevenue = latest.getTotalRevenue().doubleValue();
        double previousRevenue = previous.getTotalRevenue().doubleValue();

        if (previousRevenue == 0) {
            return 0.0;
        }

        return ((latestRevenue - previousRevenue) / previousRevenue) * 100;
    }

    private Long calculateTotalOrders(List<RevenueMetricsRepository.RevenueDaily> dailyData) {
        return dailyData.stream()
            .mapToLong(RevenueMetricsRepository.RevenueDaily::getTotalOrders)
            .sum();
    }

    private Double calculateConfidence(int forecastDay, int historicalDays) {
        // Simple confidence calculation - decreases with forecast distance
        double baseConfidence = 0.95;
        double decayRate = 0.05;
        return baseConfidence * Math.exp(-decayRate * forecastDay);
    }

    // Inner classes for DTOs
    public static class RevenueSummary {
        private List<RevenueMetricsRepository.RevenueDaily> dailyData;
        private BigDecimal totalRevenue;
        private BigDecimal totalProfit;
        private BigDecimal averageOrderValue;
        private Double growthRate;
        private Long totalOrders;

        // Getters and setters
        public List<RevenueMetricsRepository.RevenueDaily> getDailyData() { return dailyData; }
        public void setDailyData(List<RevenueMetricsRepository.RevenueDaily> dailyData) { this.dailyData = dailyData; }
        public BigDecimal getTotalRevenue() { return totalRevenue; }
        public void setTotalRevenue(BigDecimal totalRevenue) { this.totalRevenue = totalRevenue; }
        public BigDecimal getTotalProfit() { return totalProfit; }
        public void setTotalProfit(BigDecimal totalProfit) { this.totalProfit = totalProfit; }
        public BigDecimal getAverageOrderValue() { return averageOrderValue; }
        public void setAverageOrderValue(BigDecimal averageOrderValue) { this.averageOrderValue = averageOrderValue; }
        public Double getGrowthRate() { return growthRate; }
        public void setGrowthRate(Double growthRate) { this.growthRate = growthRate; }
        public Long getTotalOrders() { return totalOrders; }
        public void setTotalOrders(Long totalOrders) { this.totalOrders = totalOrders; }
    }

    public static class RevenueForecast {
        private List<ForecastPoint> forecastPoints;
        private Double averageGrowthRate;
        private String method;

        public static class ForecastPoint {
            private Instant day;
            private BigDecimal predictedRevenue;
            private Double confidence;

            // Getters and setters
            public Instant getDay() { return day; }
            public void setDay(Instant day) { this.day = day; }
            public BigDecimal getPredictedRevenue() { return predictedRevenue; }
            public void setPredictedRevenue(BigDecimal predictedRevenue) { this.predictedRevenue = predictedRevenue; }
            public Double getConfidence() { return confidence; }
            public void setConfidence(Double confidence) { this.confidence = confidence; }
        }

        // Getters and setters
        public List<ForecastPoint> getForecastPoints() { return forecastPoints; }
        public void setForecastPoints(List<ForecastPoint> forecastPoints) { this.forecastPoints = forecastPoints; }
        public Double getAverageGrowthRate() { return averageGrowthRate; }
        public void setAverageGrowthRate(Double averageGrowthRate) { this.averageGrowthRate = averageGrowthRate; }
        public String getMethod() { return method; }
        public void setMethod(String method) { this.method = method; }
    }
}
