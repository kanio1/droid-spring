/**
 * AI-Generated Customer Insight
 *
 * Represents AI-analyzed insights about customer behavior, preferences, and predictions
 */

package com.droid.bss.domain.ai;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "customer_insights")
@EntityListeners(AuditingEntityListener.class)
public class CustomerInsight {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "customer_id", nullable = false)
    private String customerId;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "insight_type", nullable = false)
    private InsightType insightType;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", length = 2000)
    private String description;

    @Column(name = "confidence_score", precision = 5, scale = 4)
    private BigDecimal confidenceScore;

    @ElementCollection
    @CollectionTable(
        name = "insight_data",
        joinColumns = {
            @JoinColumn(name = "insight_id"),
            @JoinColumn(name = "tenant_id")
        }
    )
    @MapKeyColumn(name = "data_key")
    @Column(name = "data_value", length = 500)
    private Map<String, String> data;

    @ElementCollection
    @CollectionTable(
        name = "insight_metrics",
        joinColumns = {
            @JoinColumn(name = "insight_id"),
            @JoinColumn(name = "tenant_id")
        }
    )
    private List<InsightMetric> metrics;

    @Column(name = "priority")
    private Integer priority = 0;

    @Column(name = "category")
    private String category;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private InsightStatus status = InsightStatus.ACTIVE;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "model_version")
    private String modelVersion;

    @Column(name = "model_name")
    private String modelName;

    @Column(name = "model_type")
    @Enumerated(EnumType.STRING)
    private ModelType modelType;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "viewed_at")
    private LocalDateTime viewedAt;

    @Column(name = "dismissed_at")
    private LocalDateTime dismissedAt;

    public enum InsightType {
        BEHAVIORAL_PATTERN,
        PURCHASE_PREDICTION,
        CHURN_RISK,
        CROSS_SELL_OPPORTUNITY,
        CUSTOMER_SATISFACTION,
        LIFETIME_VALUE,
        RECOMMENDATION,
        ANOMALY_DETECTION,
        SEGMENTATION,
        TREND_ANALYSIS
    }

    public enum InsightStatus {
        ACTIVE,
        VIEWED,
        DISMISSED,
        EXPIRED,
        IMPLEMENTED
    }

    public enum ModelType {
        PREDICTION,
        CLASSIFICATION,
        CLUSTERING,
        RECOMMENDATION,
        ANOMALY_DETECTION,
        SENTIMENT_ANALYSIS,
        NLP
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public InsightType getInsightType() {
        return insightType;
    }

    public void setInsightType(InsightType insightType) {
        this.insightType = insightType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(BigDecimal confidenceScore) {
        this.confidenceScore = confidenceScore;
    }

    public Map<String, String> getData() {
        return data;
    }

    public void setData(Map<String, String> data) {
        this.data = data;
    }

    public List<InsightMetric> getMetrics() {
        return metrics;
    }

    public void setMetrics(List<InsightMetric> metrics) {
        this.metrics = metrics;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public InsightStatus getStatus() {
        return status;
    }

    public void setStatus(InsightStatus status) {
        this.status = status;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public void setModelVersion(String modelVersion) {
        this.modelVersion = modelVersion;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public ModelType getModelType() {
        return modelType;
    }

    public void setModelType(ModelType modelType) {
        this.modelType = modelType;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getViewedAt() {
        return viewedAt;
    }

    public void setViewedAt(LocalDateTime viewedAt) {
        this.viewedAt = viewedAt;
    }

    public LocalDateTime getDismissedAt() {
        return dismissedAt;
    }

    public void setDismissedAt(LocalDateTime dismissedAt) {
        this.dismissedAt = dismissedAt;
    }

    // Business logic methods

    public boolean isActive() {
        return status == InsightStatus.ACTIVE;
    }

    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isHighConfidence() {
        return confidenceScore != null && confidenceScore.compareTo(new BigDecimal("0.8")) >= 0;
    }

    public boolean isHighPriority() {
        return priority != null && priority >= 8;
    }

    public void markAsViewed() {
        this.status = InsightStatus.VIEWED;
        this.viewedAt = LocalDateTime.now();
    }

    public void markAsDismissed() {
        this.status = InsightStatus.DISMISSED;
        this.dismissedAt = LocalDateTime.now();
    }

    public void markAsImplemented() {
        this.status = InsightStatus.IMPLEMENTED;
    }

    @Embeddable
    public static class InsightMetric {
        @Column(name = "metric_name", nullable = false)
        private String name;

        @Column(name = "metric_value", nullable = false)
        private String value;

        @Column(name = "metric_unit")
        private String unit;

        @Column(name = "metric_change")
        private String change;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getUnit() {
            return unit;
        }

        public void setUnit(String unit) {
            this.unit = unit;
        }

        public String getChange() {
            return change;
        }

        public void setChange(String change) {
            this.change = change;
        }
    }
}
