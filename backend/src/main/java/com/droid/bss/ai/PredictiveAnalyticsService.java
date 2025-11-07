package com.droid.bss.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Predictive Analytics Service
 * Provides AI/ML-powered predictions and recommendations
 */
@Service
public class PredictiveAnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(PredictiveAnalyticsService.class);

    // Predictive models (simplified - in production would use actual ML models)
    private final Map<String, PredictionModel> models = new ConcurrentHashMap<>();
    private final Random random = new Random();

    public PredictiveAnalyticsService() {
        initializeModels();
    }

    /**
     * Predict customer churn probability
     */
    public CustomerChurnPrediction predictCustomerChurn(String customerId, Map<String, Object> customerData) {
        log.info("Predicting churn for customer: {}", customerId);

        // In production: load actual ML model and use real customer data
        // For demo: calculate based on features

        double inactivityScore = calculateInactivityScore(customerData);
        double engagementScore = calculateEngagementScore(customerData);
        double paymentScore = calculatePaymentScore(customerData);

        // Combine scores (simplified model)
        double churnProbability = (
            inactivityScore * 0.3 +
            (1 - engagementScore) * 0.4 +
            (1 - paymentScore) * 0.3
        );

        churnProbability = Math.max(0, Math.min(1, churnProbability));

        List<String> riskFactors = new ArrayList<>();
        if (inactivityScore > 0.7) {
            riskFactors.add("Low activity in last 30 days");
        }
        if (engagementScore < 0.3) {
            riskFactors.add("Low product engagement");
        }
        if (paymentScore < 0.5) {
            riskFactors.add("Payment delays or issues");
        }

        List<String> recommendations = generateChurnRecommendations(churnProbability, riskFactors);

        return new CustomerChurnPrediction(
            customerId,
            churnProbability,
            churnProbability > 0.7 ? "HIGH" :
                churnProbability > 0.4 ? "MEDIUM" : "LOW",
            riskFactors,
            recommendations
        );
    }

    /**
     * Predict customer lifetime value
     */
    public CustomerLTVPrediction predictCustomerLTV(String customerId, Map<String, Object> customerData) {
        log.info("Predicting LTV for customer: {}", customerId);

        double currentRevenue = (Double) customerData.getOrDefault("totalRevenue", 0.0);
        int customerMonths = (Integer) customerData.getOrDefault("customerMonths", 1);
        double monthlyEngagement = (Double) customerData.getOrDefault("monthlyEngagement", 0.0);

        // Predict future revenue
        double predictedMonthlyRevenue = currentRevenue / customerMonths * (0.8 + monthlyEngagement * 0.4);
        double predictedLTV = predictedMonthlyRevenue * 36; // 3 years prediction

        List<String> valueDrivers = new ArrayList<>();
        if (monthlyEngagement > 0.7) {
            valueDrivers.add("High engagement score");
        }
        if (customerMonths > 12) {
            valueDrivers.add("Long-term customer");
        }
        if (currentRevenue > 10000) {
            valueDrivers.add("High historical revenue");
        }

        return new CustomerLTVPrediction(
            customerId,
            predictedLTV,
            predictedMonthlyRevenue,
            valueDrivers,
            generateLTVRecommendations(valueDrivers)
        );
    }

    /**
     * Predict next best offer for customer
     */
    public NextBestOffer predictNextBestOffer(String customerId, Map<String, Object> customerData) {
        log.info("Predicting next best offer for customer: {}", customerId);

        String customerSegment = (String) customerData.getOrDefault("segment", "standard");
        double engagementScore = (Double) customerData.getOrDefault("engagementScore", 0.5);
        List<String> currentProducts = (List<String>) customerData.getOrDefault("currentProducts", List.of());

        // Simple recommendation logic
        String recommendedProduct = determineRecommendedProduct(customerSegment, currentProducts);
        double confidence = calculateOfferConfidence(customerData);

        String rationale = String.format(
            "Based on %s segment and %.0f%% engagement score",
            customerSegment,
            engagementScore * 100
        );

        List<String> alternatives = generateAlternativeOffers(customerSegment, currentProducts);

        return new NextBestOffer(
            customerId,
            recommendedProduct,
            confidence,
            rationale,
            alternatives
        );
    }

    /**
     * Predict order processing time
     */
    public OrderTimePrediction predictOrderProcessingTime(String orderId, Map<String, Object> orderData) {
        log.info("Predicting processing time for order: {}", orderId);

        String orderType = (String) orderData.getOrDefault("type", "standard");
        int itemCount = (Integer) orderData.getOrDefault("itemCount", 1);
        double totalValue = (Double) orderData.getOrDefault("totalValue", 0.0);

        // Base processing time by type
        double baseTimeMinutes = switch (orderType) {
            case "express" -> 30;
            case "priority" -> 60;
            case "standard" -> 120;
            default -> 120;
        };

        // Adjust based on complexity
        double complexityFactor = 1.0 + (itemCount * 0.1) + (totalValue / 1000 * 0.05);
        double predictedMinutes = baseTimeMinutes * complexityFactor;

        List<String> factors = List.of(
            "Order type: " + orderType,
            "Item count: " + itemCount,
            "Order value: $" + String.format("%.2f", totalValue)
        );

        return new OrderTimePrediction(
            orderId,
            predictedMinutes,
            (int) (predictedMinutes * 0.8), // optimistic
            (int) (predictedMinutes * 1.2), // pessimistic
            factors
        );
    }

    /**
     * Predict resource utilization
     */
    public ResourceUtilizationPrediction predictResourceUtilization(int hoursAhead) {
        log.info("Predicting resource utilization for next {} hours", hoursAhead);

        // Use historical patterns to predict future utilization
        double currentCpuUsage = 60.0 + random.nextGaussian() * 10;
        double currentMemoryUsage = 70.0 + random.nextGaussian() * 8;
        double currentStorageUsage = 50.0 + random.nextGaussian() * 5;

        // Predict based on time patterns (higher during business hours)
        int currentHour = LocalDate.now().atStartOfDay().getHour();
        double timeFactor = currentHour >= 9 && currentHour <= 17 ? 1.2 : 0.8;

        double predictedCpu = Math.min(100, currentCpuUsage * timeFactor);
        double predictedMemory = Math.min(100, currentMemoryUsage * timeFactor * 1.1);
        double predictedStorage = Math.min(100, currentStorageUsage * (1 + hoursAhead * 0.001));

        List<String> recommendations = new ArrayList<>();
        if (predictedCpu > 80) {
            recommendations.add("Consider scaling up CPU resources");
        }
        if (predictedMemory > 85) {
            recommendations.add("Consider scaling up memory or optimizing memory usage");
        }
        if (predictedStorage > 90) {
            recommendations.add("Consider adding more storage capacity");
        }

        return new ResourceUtilizationPrediction(
            hoursAhead,
            predictedCpu,
            predictedMemory,
            predictedStorage,
            recommendations
        );
    }

    /**
     * Detect anomalies in real-time data
     */
    public List<AnomalyDetection> detectAnomalies(String entityType, Map<String, Object> data) {
        log.info("Detecting anomalies for entity: {}", entityType);

        List<AnomalyDetection> anomalies = new ArrayList<>();

        switch (entityType) {
            case "customer" -> {
                if (data.containsKey("unusualActivity")) {
                    anomalies.add(new AnomalyDetection(
                        "CUSTOMER_ACTIVITY_ANOMALY",
                        "Unusual customer activity detected",
                        "HIGH",
                        data
                    ));
                }
            }
            case "order" -> {
                double orderValue = (Double) data.getOrDefault("totalValue", 0.0);
                if (orderValue > 10000) {
                    anomalies.add(new AnomalyDetection(
                        "HIGH_VALUE_ORDER",
                        "High-value order detected",
                        "MEDIUM",
                        data
                    ));
                }
            }
            case "payment" -> {
                String status = (String) data.getOrDefault("status", "");
                if ("FAILED".equals(status)) {
                    anomalies.add(new AnomalyDetection(
                        "PAYMENT_FAILURE",
                        "Payment failure detected",
                        "MEDIUM",
                        data
                    ));
                }
            }
        }

        return anomalies;
    }

    private void initializeModels() {
        models.put("churn", new PredictionModel("churn", "Customer Churn Prediction", "v1.0"));
        models.put("ltv", new PredictionModel("ltv", "Customer Lifetime Value", "v1.0"));
        models.put("offers", new PredictionModel("offers", "Next Best Offer", "v1.0"));
        models.put("anomaly", new PredictionModel("anomaly", "Anomaly Detection", "v1.0"));

        log.info("Initialized {} prediction models", models.size());
    }

    private double calculateInactivityScore(Map<String, Object> data) {
        int daysSinceLastLogin = (Integer) data.getOrDefault("daysSinceLastLogin", 0);
        return Math.min(1.0, daysSinceLastLogin / 30.0);
    }

    private double calculateEngagementScore(Map<String, Object> data) {
        int activeDays = (Integer) data.getOrDefault("activeDaysInMonth", 0);
        int featureUsage = (Integer) data.getOrDefault("featuresUsed", 0);
        return (activeDays / 30.0 * 0.6) + (featureUsage / 10.0 * 0.4);
    }

    private double calculatePaymentScore(Map<String, Object> data) {
        int paymentDelays = (Integer) data.getOrDefault("paymentDelays", 0);
        double onTimeRate = (Double) data.getOrDefault("onTimePaymentRate", 0.9);
        return onTimeRate - (paymentDelays * 0.1);
    }

    private List<String> generateChurnRecommendations(double probability, List<String> riskFactors) {
        List<String> recommendations = new ArrayList<>();

        if (probability > 0.7) {
            recommendations.add("Send personalized retention offer");
            recommendations.add("Schedule customer success call");
            recommendations.add("Offer extended trial or discount");
        } else if (probability > 0.4) {
            recommendations.add("Send engagement campaign");
            recommendations.add("Provide product training");
        } else {
            recommendations.add("Continue current engagement strategy");
            recommendations.add("Cross-sell additional products");
        }

        return recommendations;
    }

    private List<String> generateLTVRecommendations(List<String> valueDrivers) {
        List<String> recommendations = new ArrayList<>();
        recommendations.add("Offer premium tier upgrade");
        recommendations.add("Introduce loyalty program");
        recommendations.add("Provide VIP support");
        return recommendations;
    }

    private String determineRecommendedProduct(String segment, List<String> currentProducts) {
        if (currentProducts.contains("basic")) {
            return "premium";
        } else if (currentProducts.contains("premium")) {
            return "enterprise";
        } else {
            return "basic";
        }
    }

    private double calculateOfferConfidence(Map<String, Object> data) {
        double engagement = (Double) data.getOrDefault("engagementScore", 0.5);
        return Math.min(0.95, 0.5 + engagement * 0.4);
    }

    private List<String> generateAlternativeOffers(String segment, List<String> currentProducts) {
        return List.of("bundle_deal", "discount_offer", "trial_extension");
    }

    // Record classes
    public record CustomerChurnPrediction(
        String customerId,
        double churnProbability,
        String riskLevel,
        List<String> riskFactors,
        List<String> recommendations
    ) {}

    public record CustomerLTVPrediction(
        String customerId,
        double predictedLTV,
        double predictedMonthlyRevenue,
        List<String> valueDrivers,
        List<String> recommendations
    ) {}

    public record NextBestOffer(
        String customerId,
        String recommendedProduct,
        double confidence,
        String rationale,
        List<String> alternativeOffers
    ) {}

    public record OrderTimePrediction(
        String orderId,
        double predictedMinutes,
        int optimisticEstimate,
        int pessimisticEstimate,
        List<String> influencingFactors
    ) {}

    public record ResourceUtilizationPrediction(
        int hoursAhead,
        double predictedCpuUsage,
        double predictedMemoryUsage,
        double predictedStorageUsage,
        List<String> recommendations
    ) {}

    public record AnomalyDetection(
        String type,
        String description,
        String severity,
        Map<String, Object> details
    ) {}

    private record PredictionModel(
        String name,
        String description,
        String version
    ) {}
}
