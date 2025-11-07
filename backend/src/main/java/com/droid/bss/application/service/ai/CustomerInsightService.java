/**
 * Customer Insight Service
 *
 * Business logic for AI-generated customer insights
 * Analyzes customer data and generates actionable insights
 */

package com.droid.bss.application.service.ai;

import com.droid.bss.domain.ai.CustomerInsight;
import com.droid.bss.domain.ai.CustomerInsightRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class CustomerInsightService {

    private final CustomerInsightRepository insightRepository;

    @Autowired
    public CustomerInsightService(CustomerInsightRepository insightRepository) {
        this.insightRepository = insightRepository;
    }

    /**
     * Generate churn risk insight for a customer
     */
    public CustomerInsight generateChurnRiskInsight(String customerId, String tenantId) {
        // Mock AI analysis - in real implementation, would call ML model
        Double riskScore = calculateChurnRisk(customerId);
        Double confidence = 0.75 + Math.random() * 0.2;

        CustomerInsight insight = new CustomerInsight();
        insight.setCustomerId(customerId);
        insight.setTenantId(tenantId);
        insight.setInsightType(CustomerInsight.InsightType.CHURN_RISK);
        insight.setTitle("Churn Risk Assessment");
        insight.setDescription(generateChurnRiskDescription(riskScore));
        insight.setConfidenceScore(new BigDecimal(confidence.toString()));
        insight.setPriority(riskScore > 70 ? 9 : riskScore > 50 ? 7 : 5);
        insight.setCategory("risk");
        insight.setModelName("ChurnRiskModel-v2.1");
        insight.setModelVersion("2.1.0");
        insight.setModelType(CustomerInsight.ModelType.PREDICTION);
        insight.setExpiresAt(LocalDateTime.now().plusDays(30));
        insight.setStatus(CustomerInsight.InsightStatus.ACTIVE);

        Map<String, String> data = new HashMap<>();
        data.put("risk_score", riskScore.toString());
        data.put("risk_level", getRiskLevel(riskScore));
        data.put("main_factors", "low_engagement,recent_cancellations,support_tickets");
        data.put("recommended_actions", "engagement_campaign,personal_outreach,loyalty_program");
        insight.setData(data);

        return insightRepository.save(insight);
    }

    /**
     * Generate cross-sell opportunity insight
     */
    public CustomerInsight generateCrossSellInsight(String customerId, String tenantId, String productCategory) {
        Double likelihood = 0.6 + Math.random() * 0.3;
        Double confidence = 0.70 + Math.random() * 0.25;

        CustomerInsight insight = new CustomerInsight();
        insight.setCustomerId(customerId);
        insight.setTenantId(tenantId);
        insight.setInsightType(CustomerInsight.InsightType.CROSS_SELL_OPPORTUNITY);
        insight.setTitle("Cross-Sell Opportunity: " + productCategory);
        insight.setDescription("Based on customer behavior, there's a " + String.format("%.0f%%", likelihood * 100) + " likelihood this customer would be interested in " + productCategory + " products.");
        insight.setConfidenceScore(new BigDecimal(confidence.toString()));
        insight.setPriority(8);
        insight.setCategory("revenue");
        insight.setModelName("CrossSellModel-v1.8");
        insight.setModelVersion("1.8.3");
        insight.setModelType(CustomerInsight.ModelType.RECOMMENDATION);
        insight.setExpiresAt(LocalDateTime.now().plusDays(14));
        insight.setStatus(CustomerInsight.InsightStatus.ACTIVE);

        Map<String, String> data = new HashMap<>();
        data.put("likelihood", likelihood.toString());
        data.put("product_category", productCategory);
        data.put("expected_value", "$" + (100 + Math.random() * 500));
        data.put("timeframe", "30 days");
        insight.setData(data);

        return insightRepository.save(insight);
    }

    /**
     * Generate lifetime value prediction
     */
    public CustomerInsight generateLTVInsight(String customerId, String tenantId) {
        Double predictedLTV = 5000 + Math.random() * 10000;
        Double confidence = 0.80 + Math.random() * 0.15;

        CustomerInsight insight = new CustomerInsight();
        insight.setCustomerId(customerId);
        insight.setTenantId(tenantId);
        insight.setInsightType(CustomerInsight.InsightType.LIFETIME_VALUE);
        insight.setTitle("Predicted Customer Lifetime Value");
        insight.setDescription("AI model predicts this customer's lifetime value at $" + String.format("%.2f", predictedLTV) + " over the next 12 months.");
        insight.setConfidenceScore(new BigDecimal(confidence.toString()));
        insight.setPriority(predictedLTV > 10000 ? 9 : 6);
        insight.setCategory("financial");
        insight.setModelName("LTVPredictionModel-v3.2");
        insight.setModelVersion("3.2.1");
        insight.setModelType(CustomerInsight.ModelType.PREDICTION);
        insight.setExpiresAt(LocalDateTime.now().plusDays(60));
        insight.setStatus(CustomerInsight.InsightStatus.ACTIVE);

        Map<String, String> data = new HashMap<>();
        data.put("predicted_ltv", predictedLTV.toString());
        data.put("confidence_interval", "$" + String.format("%.2f", predictedLTV * 0.8) + " - $" + String.format("%.2f", predictedLTV * 1.2));
        data.put("timeframe", "12 months");
        data.put("key_factors", "purchase_frequency,average_order_value,retention_rate");
        insight.setData(data);

        return insightRepository.save(insight);
    }

    /**
     * Generate behavioral pattern insight
     */
    public CustomerInsight generateBehavioralInsight(String customerId, String tenantId) {
        String pattern = detectBehavioralPattern(customerId);
        Double confidence = 0.70 + Math.random() * 0.25;

        CustomerInsight insight = new CustomerInsight();
        insight.setCustomerId(customerId);
        insight.setTenantId(tenantId);
        insight.setInsightType(CustomerInsight.InsightType.BEHAVIORAL_PATTERN);
        insight.setTitle("Behavioral Pattern Detected");
        insight.setDescription(pattern);
        insight.setConfidenceScore(new BigDecimal(confidence.toString()));
        insight.setPriority(6);
        insight.setCategory("behavior");
        insight.setModelName("PatternDetectionModel-v1.5");
        insight.setModelVersion("1.5.2");
        insight.setModelType(CustomerInsight.ModelType.CLUSTERING);
        insight.setExpiresAt(LocalDateTime.now().plusDays(45));
        insight.setStatus(CustomerInsight.InsightStatus.ACTIVE);

        Map<String, String> data = new HashMap<>();
        data.put("pattern_type", "purchase_frequency");
        data.put("pattern_description", pattern);
        data.put("frequency", "weekly");
        data.put("trend", "increasing");
        insight.setData(data);

        return insightRepository.save(insight);
    }

    /**
     * Generate purchase prediction insight
     */
    public CustomerInsight generatePurchasePredictionInsight(String customerId, String tenantId) {
        Double probability = 0.40 + Math.random() * 0.50;
        String timeframe = "next 7 days";
        Double confidence = 0.75 + Math.random() * 0.20;

        CustomerInsight insight = new CustomerInsight();
        insight.setCustomerId(customerId);
        insight.setTenantId(tenantId);
        insight.setInsightType(CustomerInsight.InsightType.PURCHASE_PREDICTION);
        insight.setTitle("Purchase Prediction");
        insight.setDescription("Model predicts " + String.format("%.0f%%", probability * 100) + " probability of purchase within " + timeframe + ".");
        insight.setConfidenceScore(new BigDecimal(confidence.toString()));
        insight.setPriority(probability > 0.7 ? 8 : 5);
        insight.setCategory("prediction");
        insight.setModelName("PurchasePredictionModel-v2.3");
        insight.setModelVersion("2.3.0");
        insight.setModelType(CustomerInsight.ModelType.PREDICTION);
        insight.setExpiresAt(LocalDateTime.now().plusDays(7));
        insight.setStatus(CustomerInsight.InsightStatus.ACTIVE);

        Map<String, String> data = new HashMap<>();
        data.put("probability", probability.toString());
        data.put("timeframe", timeframe);
        data.put("recommended_action", "send_personalized_offer");
        data.put("expected_value", "$" + (50 + Math.random() * 200));
        insight.setData(data);

        return insightRepository.save(insight);
    }

    /**
     * Get insights for a customer
     */
    @Transactional(readOnly = true)
    public Page<CustomerInsight> getCustomerInsights(String customerId, String tenantId, Pageable pageable) {
        return insightRepository.findByCustomerId(customerId, tenantId, pageable);
    }

    /**
     * Get active insights for a customer
     */
    @Transactional(readOnly = true)
    public List<CustomerInsight> getActiveCustomerInsights(String customerId, String tenantId) {
        return insightRepository.findActiveInsightsForCustomer(customerId, tenantId, LocalDateTime.now());
    }

    /**
     * Get high-confidence insights
     */
    @Transactional(readOnly = true)
    public List<CustomerInsight> getHighConfidenceInsights(String tenantId, Double minConfidence) {
        return insightRepository.findHighConfidenceInsights(tenantId, minConfidence);
    }

    /**
     * Get insights by type
     */
    @Transactional(readOnly = true)
    public Page<CustomerInsight> getInsightsByType(String tenantId, CustomerInsight.InsightType type, Pageable pageable) {
        return insightRepository.findByInsightType(tenantId, type, pageable);
    }

    /**
     * Mark insight as viewed
     */
    public void markInsightAsViewed(String insightId) {
        insightRepository.markAsViewed(insightId, LocalDateTime.now());
    }

    /**
     * Mark insight as dismissed
     */
    public void dismissInsight(String insightId) {
        insightRepository.markAsDismissed(insightId, LocalDateTime.now());
    }

    /**
     * Get insight statistics
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getInsightStatistics(String tenantId) {
        LocalDateTime last30Days = LocalDateTime.now().minusDays(30);
        List<Object[]> stats = insightRepository.getInsightStatistics(tenantId, last30Days);

        Map<String, Object> result = new HashMap<>();
        result.put("period_start", last30Days);
        result.put("period_end", LocalDateTime.now());
        result.put("by_type", stats.stream()
            .collect(Collectors.toMap(
                row -> ((CustomerInsight.InsightType) row[0]).name(),
                row -> Map.of(
                    "count", row[1],
                    "avg_confidence", row[2]
                )
            ))
        );

        return result;
    }

    /**
     * Batch generate insights for multiple customers
     */
    public List<CustomerInsight> batchGenerateInsights(String tenantId, List<String> customerIds) {
        List<CustomerInsight> insights = new ArrayList<>();
        for (String customerId : customerIds) {
            try {
                insights.add(generateChurnRiskInsight(customerId, tenantId));
                insights.add(generateLTVInsight(customerId, tenantId));
                if (Math.random() > 0.5) {
                    insights.add(generateCrossSellInsight(customerId, tenantId, getRandomProductCategory()));
                }
                if (Math.random() > 0.7) {
                    insights.add(generateBehavioralInsight(customerId, tenantId));
                }
            } catch (Exception e) {
                // Log error but continue with other customers
                System.err.println("Failed to generate insight for customer " + customerId + ": " + e.getMessage());
            }
        }
        return insights;
    }

    // Private helper methods

    private Double calculateChurnRisk(String customerId) {
        // Mock calculation - in real implementation, would use ML model
        return 20.0 + Math.random() * 70.0;
    }

    private String generateChurnRiskDescription(Double riskScore) {
        if (riskScore > 70) {
            return "High churn risk detected. Customer shows multiple warning signs including decreased engagement and support issues.";
        } else if (riskScore > 50) {
            return "Moderate churn risk. Monitor customer activity and consider proactive engagement.";
        } else {
            return "Low churn risk. Customer appears stable with good engagement levels.";
        }
    }

    private String getRiskLevel(Double riskScore) {
        if (riskScore > 70) return "HIGH";
        if (riskScore > 50) return "MEDIUM";
        return "LOW";
    }

    private String detectBehavioralPattern(String customerId) {
        List<String> patterns = Arrays.asList(
            "Customer shows weekly purchase pattern with increasing order values",
            "High engagement during weekend hours, low activity on weekdays",
            "Prefers mobile app over web interface (85% mobile usage)",
            "Responds well to email marketing with 15% higher conversion rate",
            "Seasonal buyer - increases activity during holiday periods"
        );
        return patterns.get((int) (Math.random() * patterns.size()));
    }

    private String getRandomProductCategory() {
        List<String> categories = Arrays.asList(
            "premium", "subscription", "accessories", "services", "enterprise"
        );
        return categories.get((int) (Math.random() * categories.size()));
    }
}
