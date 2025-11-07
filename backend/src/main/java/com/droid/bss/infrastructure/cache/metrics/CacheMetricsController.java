package com.droid.bss.infrastructure.cache.metrics;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Cache Metrics REST Controller
 *
 * Provides endpoints for monitoring cache performance
 */
@RestController
@RequestMapping("/api/v1/metrics/cache")
@Tag(name = "Cache Metrics", description = "Cache performance metrics and monitoring")
public class CacheMetricsController {

    private final CacheMetricsService metricsService;

    public CacheMetricsController(CacheMetricsService metricsService) {
        this.metricsService = metricsService;
    }

    @GetMapping("/overall")
    @Operation(summary = "Get overall cache statistics", description = "Returns aggregated cache metrics across all caches")
    public ResponseEntity<OverallCacheStatistics> getOverallStatistics() {
        OverallCacheStatistics stats = metricsService.getOverallStatistics();
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/{cacheName}")
    @Operation(summary = "Get statistics for a specific cache", description = "Returns metrics for a specific cache")
    public ResponseEntity<CacheStatistics> getCacheStatistics(@PathVariable String cacheName) {
        CacheStatistics stats = metricsService.getCacheStatistics(cacheName);
        return ResponseEntity.ok(stats);
    }

    @GetMapping
    @Operation(summary = "Get statistics for all caches", description = "Returns metrics for all caches")
    public ResponseEntity<Map<String, CacheStatistics>> getAllCacheStatistics() {
        Map<String, CacheStatistics> stats = metricsService.getAllCacheStatistics();
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/health")
    @Operation(summary = "Get cache health status", description = "Returns the health status of cache performance")
    public ResponseEntity<CacheHealthStatus> getHealthStatus() {
        CacheHealthStatus health = metricsService.getHealthStatus();
        return ResponseEntity.ok(health);
    }
}
