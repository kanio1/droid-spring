package com.droid.bss.application.service;

import com.droid.bss.domain.performance.QueryPerformanceMetrics;
import com.droid.bss.domain.performance.IndexStats;
import com.droid.bss.domain.performance.TableStats;
import com.droid.bss.domain.performance.PerformanceAnalysis;
import com.droid.bss.domain.performance.OptimizationRecommendation;
import com.droid.bss.domain.performance.IndexRecommendation;
import com.droid.bss.infrastructure.performance.QueryPerformanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Query Optimization Service
 * Analyzes performance metrics and provides optimization recommendations
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QueryOptimizationService {

    private final QueryPerformanceRepository performanceRepository;

    // Query patterns that can be optimized
    private static final Pattern SELECT_PATTERN = Pattern.compile(
        "SELECT\\s+.*?FROM\\s+(\\w+)",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );

    private static final Pattern WHERE_PATTERN = Pattern.compile(
        "WHERE\\s+(.*?)(?:GROUP|ORDER|LIMIT|$)",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );

    /**
     * Get comprehensive performance analysis
     */
    public PerformanceAnalysis getPerformanceAnalysis() {
        log.info("Generating performance analysis");

        List<QueryPerformanceMetrics> slowQueries = performanceRepository.getTopSlowQueries(10);
        List<QueryPerformanceMetrics> frequentQueries = performanceRepository.getMostFrequentQueries(10);
        List<QueryPerformanceMetrics> worstCacheQueries = performanceRepository.getWorstCacheQueries(10);
        List<IndexStats> indexStats = performanceRepository.getIndexStats();
        List<TableStats> tableStats = performanceRepository.getTableStats();

        List<OptimizationRecommendation> recommendations = generateRecommendations(
            slowQueries,
            frequentQueries,
            indexStats,
            tableStats
        );

        return PerformanceAnalysis.builder()
            .slowQueries(slowQueries)
            .frequentQueries(frequentQueries)
            .worstCacheQueries(worstCacheQueries)
            .indexStats(indexStats)
            .tableStats(tableStats)
            .recommendations(recommendations)
            .build();
    }

    /**
     * Generate optimization recommendations based on performance data
     */
    private List<OptimizationRecommendation> generateRecommendations(
            List<QueryPerformanceMetrics> slowQueries,
            List<QueryPerformanceMetrics> frequentQueries,
            List<IndexStats> indexStats,
            List<TableStats> tableStats) {

        List<OptimizationRecommendation> recommendations = new ArrayList<>();

        // Analyze slow queries
        for (QueryPerformanceMetrics query : slowQueries) {
            String grade = query.getPerformanceGrade();
            if (grade.equals("F") || grade.equals("E")) {
                String recommendation = analyzeQueryForOptimization(query.getQuery());
                if (recommendation != null) {
                    recommendations.add(OptimizationRecommendation.builder()
                        .type("PERFORMANCE")
                        .severity(grade.equals("F") ? "CRITICAL" : "HIGH")
                        .description(recommendation)
                        .queryId(query.getQueryId())
                        .impact(query.getMeanExecTime().doubleValue() / 1000.0)
                        .details(Map.of(
                            "query", query.getQuery().substring(0, Math.min(100, query.getQuery().length())),
                            "currentPerformance", grade,
                            "callCount", query.getCalls(),
                            "avgExecutionTimeMs", query.getMeanExecTime().doubleValue() / 1000.0
                        ))
                        .build());
                }
            }
        }

        // Analyze indexes
        for (IndexStats index : indexStats) {
            if (index.getIndexScans() == 0) {
                recommendations.add(OptimizationRecommendation.builder()
                    .type("INDEX")
                    .severity("LOW")
                    .description(String.format(
                        "Index %s on table %s is never used. Consider dropping it to save space.",
                        index.getIndexName(), index.getTableName()
                    ))
                    .impact(0.0)
                    .details(Map.of(
                        "indexName", index.getIndexName(),
                        "tableName", index.getTableName()
                    ))
                    .build());
            }
        }

        // Analyze tables with bloat
        for (TableStats table : tableStats) {
            if (table.getDeadRowPercent() != null && table.getDeadRowPercent().doubleValue() > 20) {
                recommendations.add(OptimizationRecommendation.builder()
                    .type("MAINTENANCE")
                    .severity("HIGH")
                    .description(String.format(
                        "Table %s has high dead row percentage (%.2f%%). Run VACUUM ANALYZE.",
                        table.getTableName(), table.getDeadRowPercent().doubleValue()
                    ))
                    .impact(table.getDeadRowPercent().doubleValue())
                    .details(Map.of(
                        "tableName", table.getTableName(),
                        "deadRowPercent", table.getDeadRowPercent().doubleValue()
                    ))
                    .build());
            }
        }

        return recommendations;
    }

    /**
     * Analyze a specific query for optimization opportunities
     */
    private String analyzeQueryForOptimization(String query) {
        if (query == null || query.isEmpty()) return null;

        StringBuilder recommendations = new StringBuilder();

        // Check for SELECT *
        if (query.matches("(?i)SELECT\\s+\\*\\s+FROM")) {
            recommendations.append("Avoid SELECT * - specify only needed columns. ");
        }

        return recommendations.length() > 0 ? recommendations.toString().trim() : null;
    }

    /**
     * Get index recommendations for a specific table
     */
    public List<IndexRecommendation> getIndexRecommendations(String tableName) {
        log.debug("Generating index recommendations for table: {}", tableName);
        return List.of();
    }
}
