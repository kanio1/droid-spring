package com.droid.bss.api.performance;

import com.droid.bss.domain.performance.PerformanceAnalysis;
import com.droid.bss.domain.performance.QueryPerformanceMetrics;
import com.droid.bss.domain.performance.DatabaseStats;
import com.droid.bss.domain.performance.IndexStats;
import com.droid.bss.domain.performance.TableStats;
import com.droid.bss.domain.performance.IndexRecommendation;
import com.droid.bss.application.service.QueryOptimizationService;
import com.droid.bss.infrastructure.performance.QueryPerformanceRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Performance Analytics API Controller
 * Provides endpoints for database performance monitoring and optimization
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/performance")
@RequiredArgsConstructor
@Tag(name = "Performance Analytics", description = "Database performance monitoring and optimization")
public class PerformanceAnalyticsController {

    private final QueryOptimizationService queryOptimizationService;
    private final QueryPerformanceRepository performanceRepository;

    @GetMapping("/analysis")
    @Operation(summary = "Get comprehensive performance analysis", description = "Returns complete performance analysis including slow queries, cache stats, and optimization recommendations")
    public ResponseEntity<PerformanceAnalysis> getPerformanceAnalysis() {
        log.info("Fetching performance analysis");
        PerformanceAnalysis analysis = queryOptimizationService.getPerformanceAnalysis();
        return ResponseEntity.ok(analysis);
    }

    @GetMapping("/database/stats")
    @Operation(summary = "Get database statistics", description = "Returns database-level performance statistics including cache hit ratio, transaction stats, and database size")
    public ResponseEntity<DatabaseStats> getDatabaseStats() {
        log.debug("Fetching database statistics");
        DatabaseStats stats = performanceRepository.getDatabaseStats();
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/queries/slow")
    @Operation(summary = "Get slow queries", description = "Returns top slow queries with performance metrics")
    public ResponseEntity<List<QueryPerformanceMetrics>> getSlowQueries(
            @RequestParam(defaultValue = "10") int limit) {
        log.debug("Fetching top {} slow queries", limit);
        List<QueryPerformanceMetrics> queries = performanceRepository.getTopSlowQueries(limit);
        return ResponseEntity.ok(queries);
    }

    @GetMapping("/queries/frequent")
    @Operation(summary = "Get most frequent queries", description = "Returns most frequently executed queries")
    public ResponseEntity<List<QueryPerformanceMetrics>> getFrequentQueries(
            @RequestParam(defaultValue = "10") int limit) {
        log.debug("Fetching top {} frequent queries", limit);
        List<QueryPerformanceMetrics> queries = performanceRepository.getMostFrequentQueries(limit);
        return ResponseEntity.ok(queries);
    }

    @GetMapping("/queries/worst-cache")
    @Operation(summary = "Get queries with worst cache performance", description = "Returns queries with low cache hit ratios")
    public ResponseEntity<List<QueryPerformanceMetrics>> getWorstCacheQueries(
            @RequestParam(defaultValue = "10") int limit) {
        log.debug("Fetching top {} queries with worst cache performance", limit);
        List<QueryPerformanceMetrics> queries = performanceRepository.getWorstCacheQueries(limit);
        return ResponseEntity.ok(queries);
    }

    @GetMapping("/indexes/stats")
    @Operation(summary = "Get index usage statistics", description = "Returns all indexes with their usage statistics")
    public ResponseEntity<List<IndexStats>> getIndexStats() {
        log.debug("Fetching index statistics");
        List<IndexStats> indexStats = performanceRepository.getIndexStats();
        return ResponseEntity.ok(indexStats);
    }

    @GetMapping("/tables/stats")
    @Operation(summary = "Get table statistics", description = "Returns all tables with statistics including bloat information")
    public ResponseEntity<List<TableStats>> getTableStats() {
        log.debug("Fetching table statistics");
        List<TableStats> tableStats = performanceRepository.getTableStats();
        return ResponseEntity.ok(tableStats);
    }

    @GetMapping("/indexes/recommendations")
    @Operation(summary = "Get index recommendations for a table", description = "Returns suggested indexes for a specific table based on query patterns")
    public ResponseEntity<List<IndexRecommendation>> getIndexRecommendations(
            @RequestParam String tableName) {
        log.debug("Fetching index recommendations for table: {}", tableName);
        List<IndexRecommendation> recommendations = queryOptimizationService.getIndexRecommendations(tableName);
        return ResponseEntity.ok(recommendations);
    }

    @PostMapping("/stats/reset")
    @Operation(summary = "Reset performance statistics", description = "Resets pg_stat_statements data (requires superuser privileges)")
    public ResponseEntity<Map<String, String>> resetStats() {
        log.warn("Resetting performance statistics");
        performanceRepository.resetQueryStats();

        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Performance statistics have been reset");
        response.put("timestamp", java.time.Instant.now().toString());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    @Operation(summary = "Get performance health check", description = "Returns overall database performance health score and summary")
    public ResponseEntity<Map<String, Object>> getHealth() {
        log.debug("Checking performance health");

        DatabaseStats dbStats = performanceRepository.getDatabaseStats();
        PerformanceAnalysis analysis = queryOptimizationService.getPerformanceAnalysis();

        Map<String, Object> health = new HashMap<>();
        health.put("overallGrade", analysis.getOverallGrade());
        health.put("healthScore", dbStats.getHealthScore());
        health.put("cacheHitRatio", dbStats.getCacheHitRatio());
        health.put("totalRecommendations", analysis.getRecommendations().size());
        health.put("criticalIssues", analysis.getRecommendations().stream()
            .mapToInt(r -> r.getSeverity().equals("CRITICAL") ? 1 : 0).sum());
        health.put("highPriorityIssues", analysis.getRecommendations().stream()
            .mapToInt(r -> r.getSeverity().equals("HIGH") ? 1 : 0).sum());
        health.put("topIssueTypes", analysis.getTopIssueTypes());
        health.put("summary", analysis.getSummary());
        health.put("timestamp", java.time.Instant.now().toString());

        return ResponseEntity.ok(health);
    }
}
