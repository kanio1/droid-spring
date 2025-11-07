package com.droid.bss.domain.performance;

import lombok.Builder;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Optimization Recommendation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OptimizationRecommendation {

    private String type; // PERFORMANCE, CACHE, INDEX, MAINTENANCE
    private String severity; // CRITICAL, HIGH, MEDIUM, LOW
    private String description;
    private Long queryId;
    private Double impact; // Estimated performance impact
    private Map<String, Object> details;

    public String getSummary() {
        return String.format("[%s] %s", severity, description);
    }

    public boolean isCritical() {
        return "CRITICAL".equals(severity);
    }

    public boolean isHighPriority() {
        return "CRITICAL".equals(severity) || "HIGH".equals(severity);
    }
}
