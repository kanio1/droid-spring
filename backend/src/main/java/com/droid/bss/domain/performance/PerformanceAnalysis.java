package com.droid.bss.domain.performance;

import lombok.Builder;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Comprehensive Performance Analysis Report
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceAnalysis {

    private List<QueryPerformanceMetrics> slowQueries;
    private List<QueryPerformanceMetrics> frequentQueries;
    private List<QueryPerformanceMetrics> worstCacheQueries;
    private List<IndexStats> indexStats;
    private List<TableStats> tableStats;
    private List<OptimizationRecommendation> recommendations;

    // Performance summary
    public String getSummary() {
        int criticalCount = recommendations.stream()
            .mapToInt(r -> r.getSeverity().equals("CRITICAL") ? 1 : 0)
            .sum();

        int highCount = recommendations.stream()
            .mapToInt(r -> r.getSeverity().equals("HIGH") ? 1 : 0)
            .sum();

        int slowQueryCount = slowQueries.size();
        int unusedIndexCount = indexStats.stream()
            .mapToInt(i -> i.getIndexScans() == 0 ? 1 : 0)
            .sum();

        return String.format("Analysis Complete: %d critical, %d high priority issues, %d slow queries, %d unused indexes",
            criticalCount, highCount, slowQueryCount, unusedIndexCount);
    }

    // Performance grade (overall)
    public String getOverallGrade() {
        int criticalCount = (int) recommendations.stream()
            .filter(r -> r.getSeverity().equals("CRITICAL"))
            .count();

        int highCount = (int) recommendations.stream()
            .filter(r -> r.getSeverity().equals("HIGH"))
            .count();

        int score = 100 - (criticalCount * 20) - (highCount * 10);

        if (score >= 90) return "A";
        if (score >= 80) return "B";
        if (score >= 70) return "C";
        if (score >= 60) return "D";
        return "F";
    }

    // Top issue types
    public List<String> getTopIssueTypes() {
        return recommendations.stream()
            .collect(java.util.stream.Collectors.groupingBy(
                OptimizationRecommendation::getType,
                java.util.stream.Collectors.counting()
            ))
            .entrySet()
            .stream()
            .sorted((e1, e2) -> Long.compare(e2.getValue(), e1.getValue()))
            .limit(3)
            .map(Map.Entry::getKey)
            .collect(java.util.stream.Collectors.toList());
    }
}
