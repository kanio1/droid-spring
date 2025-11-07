package com.droid.bss.infrastructure.cache.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Cache Metrics Collector
 * Collects and publishes cache performance metrics
 */
@Component
public class CacheMetricsCollector {

    private final MeterRegistry meterRegistry;

    // Cache hit/miss counters
    private final ConcurrentHashMap<String, Counter> cacheHits;
    private final ConcurrentHashMap<String, Counter> cacheMisses;
    private final ConcurrentHashMap<String, Counter> cacheEvictions;

    // Cache timing
    private final ConcurrentHashMap<String, Timer> cacheTimers;

    // Cache size gauges
    private final ConcurrentHashMap<String, AtomicLong> cacheSizes;

    public CacheMetricsCollector(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.cacheHits = new ConcurrentHashMap<>();
        this.cacheMisses = new ConcurrentHashMap<>();
        this.cacheEvictions = new ConcurrentHashMap<>();
        this.cacheTimers = new ConcurrentHashMap<>();
        this.cacheSizes = new ConcurrentHashMap<>();
    }

    /**
     * Record cache hit
     */
    public void recordCacheHit(String cacheName) {
        Counter counter = cacheHits.computeIfAbsent(cacheName,
            name -> Counter.builder("cache.hits")
                .tag("cache", name)
                .description("Number of cache hits")
                .register(meterRegistry));
        counter.increment();
    }

    /**
     * Record cache miss
     */
    public void recordCacheMiss(String cacheName) {
        Counter counter = cacheMisses.computeIfAbsent(cacheName,
            name -> Counter.builder("cache.misses")
                .tag("cache", name)
                .description("Number of cache misses")
                .register(meterRegistry));
        counter.increment();
    }

    /**
     * Record cache eviction
     */
    public void recordCacheEviction(String cacheName) {
        Counter counter = cacheEvictions.computeIfAbsent(cacheName,
            name -> Counter.builder("cache.evictions")
                .tag("cache", name)
                .description("Number of cache evictions")
                .register(meterRegistry));
        counter.increment();
    }

    /**
     * Record cache operation timing
     */
    public Timer.Sample startCacheTimer(String cacheName) {
        Timer timer = cacheTimers.computeIfAbsent(cacheName,
            name -> Timer.builder("cache.operation.duration")
                .tag("cache", name)
                .description("Time spent on cache operations")
                .register(meterRegistry));

        Timer.Sample sample = Timer.start(meterRegistry);
        return sample;
    }

    /**
     * Stop cache timer and record duration
     */
    public void stopCacheTimer(Timer.Sample sample, String cacheName) {
        sample.stop(cacheTimers.get(cacheName));
    }

    /**
     * Update cache size gauge
     */
    public void updateCacheSize(String cacheName, long size) {
        AtomicLong sizeRef = cacheSizes.computeIfAbsent(cacheName, k -> new AtomicLong(0));
        sizeRef.set(size);

        // Register gauge if not already registered
        if (!cacheSizes.containsKey(cacheName + "_gauge")) {
            Gauge.builder("cache.size")
                .tag("cache", cacheName)
                .description("Current cache size")
                .register(meterRegistry, sizeRef, AtomicLong::get);
        }
    }

    /**
     * Calculate hit rate for a cache
     */
    public double getHitRate(String cacheName) {
        Counter hits = cacheHits.get(cacheName);
        Counter misses = cacheMisses.get(cacheName);

        if (hits == null || misses == null) {
            return 0.0;
        }

        long total = hits.count() + misses.count();
        if (total == 0) {
            return 0.0;
        }

        return hits.count() / total;
    }

    /**
     * Get overall cache statistics
     */
    public CachePerformanceSummary getSummary() {
        CachePerformanceSummary.Builder builder = CachePerformanceSummary.builder();

        cacheHits.forEach((name, counter) -> {
            double hitRate = getHitRate(name);
            builder.addCacheStats(name,
                (long) counter.count(),
                (long) cacheMisses.getOrDefault(name, Counter.builder("temp").register(meterRegistry)).count(),
                hitRate);
        });

        return builder.build();
    }
}

/**
 * Cache Performance Summary
 */
class CachePerformanceSummary {
    private final java.util.List<CacheStats> cacheStats;

    private CachePerformanceSummary(Builder builder) {
        this.cacheStats = builder.cacheStats;
    }

    public static Builder builder() {
        return new Builder();
    }

    public java.util.List<CacheStats> getCacheStats() {
        return cacheStats;
    }

    public static class Builder {
        private final java.util.List<CacheStats> cacheStats = new java.util.ArrayList<>();

        public Builder addCacheStats(String cacheName, long hits, long misses, double hitRate) {
            cacheStats.add(new CacheStats(cacheName, hits, misses, hitRate));
            return this;
        }

        public CachePerformanceSummary build() {
            return new CachePerformanceSummary(this);
        }
    }

    public static class CacheStats {
        private final String cacheName;
        private final long hits;
        private final long misses;
        private final double hitRate;

        public CacheStats(String cacheName, long hits, long misses, double hitRate) {
            this.cacheName = cacheName;
            this.hits = hits;
            this.misses = misses;
            this.hitRate = hitRate;
        }

        public String getCacheName() { return cacheName; }
        public long getHits() { return hits; }
        public long getMisses() { return misses; }
        public double getHitRate() { return hitRate; }
    }
}
