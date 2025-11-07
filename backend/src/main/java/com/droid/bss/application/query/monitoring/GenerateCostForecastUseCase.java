package com.droid.bss.application.query.monitoring;

import com.droid.bss.domain.monitoring.CostCalculation;
import com.droid.bss.domain.monitoring.CostCalculationRepository;
import com.droid.bss.domain.monitoring.CostForecast;
import com.droid.bss.domain.monitoring.CostForecastRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class GenerateCostForecastUseCase {

    private final CostCalculationRepository costCalculationRepository;
    private final CostForecastRepository costForecastRepository;

    public GenerateCostForecastUseCase(CostCalculationRepository costCalculationRepository,
                                      CostForecastRepository costForecastRepository) {
        this.costCalculationRepository = costCalculationRepository;
        this.costForecastRepository = costForecastRepository;
    }

    public List<CostForecast> generateForecast(Long customerId, String resourceType, String billingPeriod,
                                              Instant forecastStartDate, Instant forecastEndDate,
                                              int historicalMonths, String forecastModel) {
        Instant startDate = forecastStartDate.minusSeconds(30L * 24 * 60 * 60 * historicalMonths);

        List<CostCalculation> historicalData = costCalculationRepository
                .findByCustomerIdAndPeriod(customerId, startDate, forecastStartDate);

        List<CostCalculation> filteredData = historicalData.stream()
                .filter(cc -> cc.getResourceType().equals(resourceType))
                .filter(cc -> cc.getBillingPeriod().equals(billingPeriod))
                .filter(cc -> "FINAL".equals(cc.getStatus()))
                .sorted((a, b) -> a.getPeriodStart().compareTo(b.getPeriodStart()))
                .toList();

        if (filteredData.size() < 2) {
            throw new IllegalArgumentException("Insufficient historical data for forecasting. Need at least 2 data points.");
        }

        return switch (forecastModel) {
            case "LINEAR_REGRESSION" -> generateLinearRegressionForecast(
                    customerId, resourceType, billingPeriod, forecastStartDate, forecastEndDate, filteredData);
            case "MOVING_AVERAGE" -> generateMovingAverageForecast(
                    customerId, resourceType, billingPeriod, forecastStartDate, forecastEndDate, filteredData);
            default -> throw new IllegalArgumentException("Unknown forecast model: " + forecastModel);
        };
    }

    private List<CostForecast> generateLinearRegressionForecast(
            Long customerId, String resourceType, String billingPeriod,
            Instant forecastStartDate, Instant forecastEndDate,
            List<CostCalculation> historicalData) {

        List<CostForecast> forecasts = new ArrayList<>();

        int dataPoints = historicalData.size();
        double[] x = new double[dataPoints];
        double[] y = new double[dataPoints];

        for (int i = 0; i < dataPoints; i++) {
            x[i] = i;
            y[i] = historicalData.get(i).getTotalCost().doubleValue();
        }

        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
        for (int i = 0; i < dataPoints; i++) {
            sumX += x[i];
            sumY += y[i];
            sumXY += x[i] * y[i];
            sumX2 += x[i] * x[i];
        }

        double slope = (dataPoints * sumXY - sumX * sumY) / (dataPoints * sumX2 - sumX * sumX);
        double intercept = (sumY - slope * sumX) / dataPoints;

        Instant currentDate = forecastStartDate;
        long periodDays = calculatePeriodDays(billingPeriod);

        while (currentDate.isBefore(forecastEndDate) || currentDate.equals(forecastEndDate)) {
            long timeDiff = (currentDate.toEpochMilli() - forecastStartDate.toEpochMilli()) / (periodDays * 24 * 60 * 60 * 1000);
            int futureIndex = dataPoints + (int) timeDiff;
            double predictedY = slope * futureIndex + intercept;

            BigDecimal predictedCost = BigDecimal.valueOf(predictedY).round(MathContext.DECIMAL64);

            BigDecimal variance = calculateVariance(y, predictedY);
            BigDecimal stdDev = new BigDecimal(Math.sqrt(variance.doubleValue())).round(MathContext.DECIMAL64);
            BigDecimal lowerBound = predictedCost.subtract(stdDev.multiply(BigDecimal.valueOf(2)));
            BigDecimal upperBound = predictedCost.add(stdDev.multiply(BigDecimal.valueOf(2)));

            String trendDirection = slope > 0.1 ? "INCREASING" : slope < -0.1 ? "DECREASING" : "STABLE";
            Double confidenceLevel = Math.min(0.95, 0.5 + (dataPoints * 0.05));

            Instant periodEnd = currentDate.plusSeconds(periodDays * 24 * 60 * 60);

            CostForecast forecast = new CostForecast(
                    customerId, resourceType, billingPeriod,
                    currentDate, periodEnd,
                    predictedCost, lowerBound.max(BigDecimal.ZERO), upperBound,
                    trendDirection, confidenceLevel, "LINEAR_REGRESSION");

            forecasts.add(costForecastRepository.save(forecast));

            currentDate = periodEnd;
        }

        return forecasts;
    }

    private List<CostForecast> generateMovingAverageForecast(
            Long customerId, String resourceType, String billingPeriod,
            Instant forecastStartDate, Instant forecastEndDate,
            List<CostCalculation> historicalData) {

        List<CostForecast> forecasts = new ArrayList<>();

        int windowSize = Math.min(3, historicalData.size());
        BigDecimal recentAverage = historicalData.stream()
                .skip(historicalData.size() - windowSize)
                .map(CostCalculation::getTotalCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(windowSize), MathContext.DECIMAL64);

        BigDecimal variance = BigDecimal.ZERO;
        for (int i = historicalData.size() - windowSize; i < historicalData.size(); i++) {
            BigDecimal diff = historicalData.get(i).getTotalCost().subtract(recentAverage);
            variance = variance.add(diff.multiply(diff));
        }
        variance = variance.divide(BigDecimal.valueOf(windowSize), MathContext.DECIMAL64);
        BigDecimal stdDev = new BigDecimal(Math.sqrt(variance.doubleValue())).round(MathContext.DECIMAL64);

        String trendDirection = calculateTrendDirection(historicalData);

        Instant currentDate = forecastStartDate;
        long periodDays = calculatePeriodDays(billingPeriod);

        while (currentDate.isBefore(forecastEndDate) || currentDate.equals(forecastEndDate)) {
            BigDecimal predictedCost = recentAverage;
            BigDecimal lowerBound = predictedCost.subtract(stdDev.multiply(BigDecimal.valueOf(2)));
            BigDecimal upperBound = predictedCost.add(stdDev.multiply(BigDecimal.valueOf(2)));

            Double confidenceLevel = 0.7;

            Instant periodEnd = currentDate.plusSeconds(periodDays * 24 * 60 * 60);

            CostForecast forecast = new CostForecast(
                    customerId, resourceType, billingPeriod,
                    currentDate, periodEnd,
                    predictedCost, lowerBound.max(BigDecimal.ZERO), upperBound,
                    trendDirection, confidenceLevel, "MOVING_AVERAGE");

            forecasts.add(costForecastRepository.save(forecast));

            currentDate = periodEnd;
        }

        return forecasts;
    }

    private long calculatePeriodDays(String billingPeriod) {
        return switch (billingPeriod.toLowerCase()) {
            case "hourly" -> 1;
            case "daily" -> 1;
            case "weekly" -> 7;
            case "monthly" -> 30;
            case "yearly" -> 365;
            default -> 30;
        };
    }

    private BigDecimal calculateVariance(double[] values, double predicted) {
        double sum = 0;
        for (double value : values) {
            sum += Math.pow(value - predicted, 2);
        }
        return BigDecimal.valueOf(sum / values.length);
    }

    private String calculateTrendDirection(List<CostCalculation> historicalData) {
        if (historicalData.size() < 2) {
            return "STABLE";
        }

        double firstHalf = 0;
        double secondHalf = 0;
        int halfSize = historicalData.size() / 2;

        for (int i = 0; i < halfSize; i++) {
            firstHalf += historicalData.get(i).getTotalCost().doubleValue();
        }

        for (int i = halfSize; i < historicalData.size(); i++) {
            secondHalf += historicalData.get(i).getTotalCost().doubleValue();
        }

        firstHalf /= halfSize;
        secondHalf /= (historicalData.size() - halfSize);

        if (secondHalf > firstHalf * 1.1) {
            return "INCREASING";
        } else if (secondHalf < firstHalf * 0.9) {
            return "DECREASING";
        } else {
            return "STABLE";
        }
    }
}
