package com.droid.bss.infrastructure.cache.warming;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Cache Warming REST Controller
 *
 * Provides endpoints for triggering cache warming on demand
 */
@RestController
@RequestMapping("/api/v1/cache-warming")
@Tag(name = "Cache Warming", description = "Cache warming operations")
public class CacheWarmingController {

    private final CacheWarmingService cacheWarmingService;

    public CacheWarmingController(CacheWarmingService cacheWarmingService) {
        this.cacheWarmingService = cacheWarmingService;
    }

    @PostMapping("/warm/{cacheType}")
    @Operation(summary = "Warm a specific cache", description = "Triggers cache warming for a specific cache type")
    public CompletableFuture<ResponseEntity<String>> warmCache(@PathVariable CacheType cacheType) {
        return cacheWarmingService.warmCacheOnDemand(cacheType)
                .thenApply(v -> ResponseEntity.ok("Cache warming initiated for: " + cacheType));
    }

    @PostMapping("/warm-all")
    @Operation(summary = "Warm all caches", description = "Triggers cache warming for all cache types")
    public CompletableFuture<ResponseEntity<String>> warmAllCaches() {
        return cacheWarmingService.warmCacheOnDemand(CacheType.ALL)
                .thenApply(v -> ResponseEntity.ok("Cache warming initiated for all caches"));
    }

    @GetMapping("/statistics")
    @Operation(summary = "Get cache warming statistics", description = "Returns statistics about cache warming operations")
    public ResponseEntity<Map<String, CacheWarmingStatistics>> getStatistics() {
        // In a real implementation, you'd store and return actual statistics
        return ResponseEntity.ok(Map.of(
                "customer", cacheWarmingService.getStatistics()
        ));
    }
}
