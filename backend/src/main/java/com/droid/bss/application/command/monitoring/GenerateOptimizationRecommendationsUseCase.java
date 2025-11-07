package com.droid.bss.application.command.monitoring;

import com.droid.bss.domain.monitoring.CostCalculation;
import com.droid.bss.domain.monitoring.CostCalculationRepository;
import com.droid.bss.domain.monitoring.OptimizationRecommendation;
import com.droid.bss.domain.monitoring.OptimizationRecommendationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class GenerateOptimizationRecommendationsUseCase {

    private final CostCalculationRepository costCalculationRepository;
    private final OptimizationRecommendationRepository optimizationRecommendationRepository;

    public GenerateOptimizationRecommendationsUseCase(CostCalculationRepository costCalculationRepository,
                                                     OptimizationRecommendationRepository optimizationRecommendationRepository) {
        this.costCalculationRepository = costCalculationRepository;
        this.optimizationRecommendationRepository = optimizationRecommendationRepository;
    }

    public List<OptimizationRecommendation> analyzeAndRecommend(Long customerId, String resourceType,
                                                                Instant startDate, Instant endDate) {
        List<CostCalculation> calculations = costCalculationRepository
                .findByCustomerIdAndPeriod(customerId, startDate, endDate);

        List<CostCalculation> filtered = calculations.stream()
                .filter(cc -> resourceType == null || cc.getResourceType().equals(resourceType))
                .filter(cc -> "FINAL".equals(cc.getStatus()))
                .collect(Collectors.toList());

        List<OptimizationRecommendation> recommendations = new ArrayList<>();

        if (filtered.isEmpty()) {
            return recommendations;
        }

        Map<String, List<CostCalculation>> byResourceId = filtered.stream()
                .collect(Collectors.groupingBy(cc -> cc.getResourceType() + "-" + cc.getCustomerId()));

        for (Map.Entry<String, List<CostCalculation>> entry : byResourceId.entrySet()) {
            List<CostCalculation> resourceCosts = entry.getValue();
            OptimizationRecommendation rec = analyzeResource(resourceCosts, customerId);
            if (rec != null) {
                recommendations.add(optimizationRecommendationRepository.save(rec));
            }
        }

        return recommendations;
    }

    private OptimizationRecommendation analyzeResource(List<CostCalculation> costs, Long customerId) {
        if (costs.isEmpty()) {
            return null;
        }

        CostCalculation firstCost = costs.get(0);
        String resourceType = firstCost.getResourceType();
        String currency = firstCost.getCurrency();

        BigDecimal totalCost = costs.stream()
                .map(CostCalculation::getTotalCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal avgCost = totalCost.divide(BigDecimal.valueOf(costs.size()), BigDecimal.ROUND_HALF_UP);

        BigDecimal totalUsage = costs.stream()
                .map(CostCalculation::getTotalUsage)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal avgUsage = totalUsage.divide(BigDecimal.valueOf(costs.size()), BigDecimal.ROUND_HALF_UP);

        BigDecimal maxCost = costs.stream()
                .map(CostCalculation::getTotalCost)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        BigDecimal minCost = costs.stream()
                .map(CostCalculation::getTotalCost)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        BigDecimal costVariance = calculateVariance(costs, avgCost);

        String recommendationType = determineRecommendationType(avgUsage, avgCost, maxCost, minCost, costVariance);
        if (recommendationType == null) {
            return null;
        }

        String severity = calculateSeverity(avgCost, costVariance);
        String title = generateTitle(recommendationType, resourceType);
        String description = generateDescription(recommendationType, resourceType, avgUsage, avgCost, costVariance);
        BigDecimal potentialSavings = calculatePotentialSavings(recommendationType, avgCost, costVariance);
        BigDecimal projectedCost = avgCost.subtract(potentialSavings);

        return new OptimizationRecommendation(
                customerId,
                resourceType,
                resourceType + "-" + customerId,
                recommendationType,
                severity,
                title,
                description,
                potentialSavings,
                currency,
                avgCost,
                projectedCost,
                "NEW",
                Instant.now()
        );
    }

    private String determineRecommendationType(BigDecimal avgUsage, BigDecimal avgCost, BigDecimal maxCost,
                                              BigDecimal minCost, BigDecimal variance) {
        if (variance.compareTo(avgCost.multiply(BigDecimal.valueOf(0.5))) > 0) {
            return "SCHEDULE_SHUTDOWN";
        }

        if (minCost.compareTo(avgCost.multiply(BigDecimal.valueOf(0.3))) < 0) {
            return "SCALE_DOWN";
        }

        if (maxCost.compareTo(avgCost.multiply(BigDecimal.valueOf(2.0))) > 0) {
            return "RIGHT_SIZE";
        }

        if (avgUsage.compareTo(BigDecimal.valueOf(50)) < 0) {
            return "SCALE_DOWN";
        }

        return null;
    }

    private String calculateSeverity(BigDecimal avgCost, BigDecimal variance) {
        BigDecimal ratio = variance.divide(avgCost, BigDecimal.ROUND_HALF_UP);

        if (ratio.compareTo(BigDecimal.valueOf(0.5)) > 0) {
            return "HIGH";
        } else if (ratio.compareTo(BigDecimal.valueOf(0.2)) > 0) {
            return "MEDIUM";
        } else {
            return "LOW";
        }
    }

    private String generateTitle(String recommendationType, String resourceType) {
        return switch (recommendationType) {
            case "RIGHT_SIZE" -> "Right-size " + resourceType + " instance";
            case "SCALE_DOWN" -> "Scale down " + resourceType + " to reduce costs";
            case "SCALE_UP" -> "Scale up " + resourceType + " for better performance";
            case "SCHEDULE_SHUTDOWN" -> "Schedule " + resourceType + " shutdown during low usage";
            case "SWITCH_PLAN" -> "Switch to a different pricing plan";
            default -> "Cost optimization opportunity";
        };
    }

    private String generateDescription(String recommendationType, String resourceType, BigDecimal avgUsage,
                                      BigDecimal avgCost, BigDecimal variance) {
        return switch (recommendationType) {
            case "RIGHT_SIZE" ->
                "Your " + resourceType + " has high cost variance (€" + variance + "). " +
                "Consider right-sizing to match actual usage patterns and reduce costs by optimizing resource allocation.";
            case "SCALE_DOWN" ->
                "Your " + resourceType + " has low average usage (" + avgUsage + "%) and is costing €" + avgCost + " on average. " +
                "Scale down to a smaller instance type to reduce costs by up to 40%.";
            case "SCALE_UP" ->
                "Your " + resourceType + " is running at high utilization. " +
                "Consider scaling up to improve performance and avoid throttling.";
            case "SCHEDULE_SHUTDOWN" ->
                "Your " + resourceType + " has very low cost during some periods (variance: €" + variance + "). " +
                "Schedule automated shutdowns during non-business hours to reduce costs by up to 60%.";
            case "SWITCH_PLAN" ->
                "Based on your usage patterns, switching to a different pricing plan could save you money. " +
                "Consider reserved instances or spot instances for predictable workloads.";
            default -> "Cost optimization opportunity identified for " + resourceType;
        };
    }

    private BigDecimal calculatePotentialSavings(String recommendationType, BigDecimal avgCost, BigDecimal variance) {
        return switch (recommendationType) {
            case "RIGHT_SIZE" -> variance.multiply(BigDecimal.valueOf(0.3));
            case "SCALE_DOWN" -> avgCost.multiply(BigDecimal.valueOf(0.4));
            case "SCALE_UP" -> BigDecimal.ZERO;
            case "SCHEDULE_SHUTDOWN" -> avgCost.multiply(BigDecimal.valueOf(0.6));
            case "SWITCH_PLAN" -> avgCost.multiply(BigDecimal.valueOf(0.2));
            default -> BigDecimal.ZERO;
        };
    }

    private BigDecimal calculateVariance(List<CostCalculation> costs, BigDecimal mean) {
        double sum = costs.stream()
                .mapToDouble(c -> c.getTotalCost().subtract(mean).pow(2).doubleValue())
                .sum();
        return BigDecimal.valueOf(sum / costs.size());
    }
}
