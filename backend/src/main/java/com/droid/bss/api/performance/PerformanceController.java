package com.droid.bss.api.performance;

import com.droid.bss.domain.monitoring.*;
import com.droid.bss.infrastructure.observability.PerformanceMonitoringService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/performance")
@RequiredArgsConstructor
@Tag(name = "Performance Monitoring", description = "Database performance monitoring and query analysis")
public class PerformanceController {

    private final PerformanceMonitoringService performanceService;

    @GetMapping("/queries/active")
    @Operation(summary = "Get active queries", description = "Retrieve currently active database queries")
    public ResponseEntity<List<ActiveQuery>> getActiveQueries() {
        try {
            log.info("Fetching active queries");
            List<ActiveQuery> queries = performanceService.getActiveQueries();
            return ResponseEntity.ok(queries);
        } catch (Exception e) {
            log.error("Error fetching active queries", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/queries/long-running")
    @Operation(summary = "Get long-running queries", description = "Retrieve queries that have been running longer than threshold")
    public ResponseEntity<List<LongRunningQuery>> getLongRunningQueries(
            @RequestParam(defaultValue = "30") int thresholdSeconds) {
        try {
            log.info("Fetching long-running queries with threshold: {} seconds", thresholdSeconds);
            List<LongRunningQuery> queries = performanceService.getLongRunningQueries(thresholdSeconds);
            return ResponseEntity.ok(queries);
        } catch (Exception e) {
            log.error("Error fetching long-running queries", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/locks")
    @Operation(summary = "Get lock information", description = "Retrieve information about database locks and blocking")
    public ResponseEntity<List<LockInfo>> getLockInformation() {
        try {
            log.info("Fetching lock information");
            List<LockInfo> locks = performanceService.getLockInformation();
            return ResponseEntity.ok(locks);
        } catch (Exception e) {
            log.error("Error fetching lock information", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/deadlocks")
    @Operation(summary = "Get deadlock information", description = "Retrieve recent deadlock events")
    public ResponseEntity<List<DeadlockInfo>> getDeadlockInformation() {
        try {
            log.info("Fetching deadlock information");
            List<DeadlockInfo> deadlocks = performanceService.getDeadlockInformation();
            return ResponseEntity.ok(deadlocks);
        } catch (Exception e) {
            log.error("Error fetching deadlock information", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/statistics/database")
    @Operation(summary = "Get database statistics", description = "Retrieve overall database performance statistics")
    public ResponseEntity<DatabaseStatistics> getDatabaseStatistics() {
        try {
            log.info("Fetching database statistics");
            DatabaseStatistics stats = performanceService.getDatabaseStatistics();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Error fetching database statistics", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/statistics/tables")
    @Operation(summary = "Get table statistics", description = "Retrieve statistics for all user tables")
    public ResponseEntity<List<TableStatistics>> getTableStatistics() {
        try {
            log.info("Fetching table statistics");
            List<TableStatistics> tables = performanceService.getTableStatistics();
            return ResponseEntity.ok(tables);
        } catch (Exception e) {
            log.error("Error fetching table statistics", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/statistics/indexes")
    @Operation(summary = "Get index statistics", description = "Retrieve statistics for all user indexes")
    public ResponseEntity<List<IndexStatistics>> getIndexStatistics() {
        try {
            log.info("Fetching index statistics");
            List<IndexStatistics> indexes = performanceService.getIndexStatistics();
            return ResponseEntity.ok(indexes);
        } catch (Exception e) {
            log.error("Error fetching index statistics", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/statistics/queries")
    @Operation(summary = "Get query statistics", description = "Retrieve performance statistics for executed queries")
    public ResponseEntity<List<QueryStatistics>> getQueryStatistics(
            @RequestParam(defaultValue = "50") int limit) {
        try {
            log.info("Fetching query statistics with limit: {}", limit);
            List<QueryStatistics> queries = performanceService.getQueryStatistics(limit);
            return ResponseEntity.ok(queries);
        } catch (Exception e) {
            log.error("Error fetching query statistics", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/metrics")
    @Operation(summary = "Get system metrics", description = "Retrieve current system-level performance metrics")
    public ResponseEntity<SystemMetrics> getSystemMetrics() {
        try {
            log.info("Fetching system metrics");
            SystemMetrics metrics = performanceService.getSystemMetrics();
            return ResponseEntity.ok(metrics);
        } catch (Exception e) {
            log.error("Error fetching system metrics", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/alerts")
    @Operation(summary = "Get performance alerts", description = "Retrieve current performance alerts and warnings")
    public ResponseEntity<List<PerformanceAlert>> getPerformanceAlerts() {
        try {
            log.info("Fetching performance alerts");
            List<PerformanceAlert> alerts = performanceService.getPerformanceAlerts();
            return ResponseEntity.ok(alerts);
        } catch (Exception e) {
            log.error("Error fetching performance alerts", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/queries/{pid}/kill")
    @Operation(summary = "Kill query", description = "Terminate a running query by PID")
    public ResponseEntity<Void> killQuery(@PathVariable Long pid) {
        try {
            log.warn("Killing query with PID: {}", pid);
            performanceService.killQuery(pid);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error killing query with PID: {}", pid, e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
