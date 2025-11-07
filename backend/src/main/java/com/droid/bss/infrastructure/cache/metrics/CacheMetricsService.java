package com.droid.bss.infrastructure.cache.metrics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Cache Metrics Service
 *
 * Tracks cache performance metrics: hits, misses, evictions, etc.
 */
@Service
public class CacheMetricsService {

    private static final Logger log = LoggerFactory.getLogger(CacheMetricsService.class);

    private final CacheManager cacheManager;
    private final ConcurrentHashMap<String, CacheStatistics> cacheStatistics = new ConcurrentHashMap<>();
    private final AtomicLong totalCacheHits = new AtomicLong(0);
    private final AtomicLong totalCacheMisses = new AtomicLong(0);
    private final AtomicLong totalCacheEvictions = new AtomicLong(0);
    private final AtomicLong totalCachePuts = new AtomicLong(0);

    public CacheMetricsService(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    /**
     * Record a cache hit
     */
    public void recordCacheHit(String cacheName) {
        totalCacheHits.incrementAndGet();
        getCacheStatistics(cacheName).recordHit();
        log.debug("Cache hit: {}", cacheName);
    }

    /**
     * Record a cache miss
     */
    public void recordCacheMiss(String cacheName) {
        totalCacheMisses.incrementAndGet();
        getCacheStatistics(cacheName).recordMiss();
        log.debug("Cache miss: {}", cacheName);
    }

    /**
     * Record a cache put
     */
    public void recordCachePut(String cacheName) {
        totalCachePuts.incrementAndGet();
        getCacheStatistics(cacheName).recordPut();
        log.debug("Cache put: {}", cacheName);
    }

    /**
     * Record a cache eviction
     */
    public void recordCacheEviction(String cacheName) {
        totalCacheEvictions.incrementAndGet();
        getCacheStatistics(cacheName).recordEviction();
        log.debug("Cache eviction: {}", cacheName);
    }

    /**
     * Get cache statistics by name
     */
    public CacheStatistics getCacheStatistics(String cacheName) {
        return cacheStatistics.computeIfAbsent(cacheName, CacheStatistics::new);
    }

    /**
     * Get all cache statistics
     */
    public ConcurrentHashMap<String, CacheStatistics> getAllCacheStatistics() {
        return new ConcurrentHashMap<>(cacheStatistics);
    }

    /**
     * Get overall cache statistics
     */
    public OverallCacheStatistics getOverallStatistics() {
        return new OverallCacheStatistics(
                totalCacheHits.get(),
                totalCacheMisses.get(),
                totalCacheEvictions.get(),
                totalCachePuts.get(),
                cacheStatistics.size()
        );
    }

    /**
     * Calculate hit rate percentage
     */
    public double getCacheHitRate() {
        long hits = totalCacheHits.get();
        long misses = totalCacheMisses.get();
        long total = hits + misses;

        if (total == 0) {
            return 0.0;
        }

        return (double) hits / total * 100.0;
    }

    /**
     * Get cache health status
     */
    public CacheHealthStatus getHealthStatus() {
        double hitRate = getCacheHitRate();

        String status;
        String message;

        if (hitRate >= 80.0) {
            status = "HEALTHY";
            message = "Cache performing well with " + String.format("%.2f%%", hitRate) + " hit rate";
        } else if (hitRate >= 60.0) {
            status = "WARNING";
            message = "Cache hit rate is suboptimal: " + String.format("%.2f%%", hitRate);
        } else {
            status = "CRITICAL";
            message = "Cache hit rate is critically low: " + String.format("%.2f%%", hitRate);
        }

        return new CacheHealthStatus(status, message, hitRate);
    }
}
