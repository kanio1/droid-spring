package com.droid.bss.infrastructure.database.sharding;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Statistics for shard routing operations.
 *
 * @since 1.0
 */
public class ShardRoutingStats {

    private final Instant createdAt;
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong cacheHits = new AtomicLong(0);
    private final AtomicLong cacheMisses = new AtomicLong(0);
    private final AtomicLong routeCalculations = new AtomicLong(0);
    private final AtomicLong errorCount = new AtomicLong(0);

    private final ConcurrentMap<String, AtomicLong> requestsByShard = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, AtomicLong> errorsByShard = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Long> lastRequestTime = new ConcurrentHashMap<>();

    /**
     * Creates a new ShardRoutingStats.
     */
    public ShardRoutingStats() {
        this.createdAt = Instant.now();
    }

    /**
     * Records a successful routing.
     *
     * @param shardId the shard ID
     */
    public void recordRouting(String shardId) {
        totalRequests.incrementAndGet();
        requestsByShard.computeIfAbsent(shardId, k -> new AtomicLong(0)).incrementAndGet();
        lastRequestTime.put(shardId, System.currentTimeMillis());
    }

    /**
     * Records a cache hit.
     */
    public void recordCacheHit() {
        cacheHits.incrementAndGet();
    }

    /**
     * Records a cache miss.
     */
    public void recordCacheMiss() {
        cacheMisses.incrementAndGet();
    }

    /**
     * Records a route calculation.
     */
    public void recordRouteCalculation() {
        routeCalculations.incrementAndGet();
    }

    /**
     * Records an error.
     *
     * @param shardId the shard ID (may be null)
     */
    public void recordError(String shardId) {
        errorCount.incrementAndGet();
        if (shardId != null) {
            errorsByShard.computeIfAbsent(shardId, k -> new AtomicLong(0)).incrementAndGet();
        }
    }

    /**
     * Gets the total number of requests.
     *
     * @return the total requests
     */
    public long getTotalRequests() {
        return totalRequests.get();
    }

    /**
     * Gets the number of cache hits.
     *
     * @return the cache hits
     */
    public long getCacheHits() {
        return cacheHits.get();
    }

    /**
     * Gets the number of cache misses.
     *
     * @return the cache misses
     */
    public long getCacheMisses() {
        return cacheMisses.get();
    }

    /**
     * Gets the number of route calculations.
     *
     * @return the route calculations
     */
    public long getRouteCalculations() {
        return routeCalculations.get();
    }

    /**
     * Gets the number of errors.
     *
     * @return the error count
     */
    public long getErrorCount() {
        return errorCount.get();
    }

    /**
     * Gets the requests by shard.
     *
     * @return the map of shard ID to request count
     */
    public ConcurrentMap<String, Long> getRequestsByShard() {
        ConcurrentMap<String, Long> result = new ConcurrentHashMap<>();
        requestsByShard.forEach((k, v) -> result.put(k, v.get()));
        return result;
    }

    /**
     * Gets the errors by shard.
     *
     * @return the map of shard ID to error count
     */
    public ConcurrentMap<String, Long> getErrorsByShard() {
        ConcurrentMap<String, Long> result = new ConcurrentHashMap<>();
        errorsByShard.forEach((k, v) -> result.put(k, v.get()));
        return result;
    }

    /**
     * Gets the last request time for a shard.
     *
     * @param shardId the shard ID
     * @return the last request time (epoch millis) or -1 if not found
     */
    public long getLastRequestTime(String shardId) {
        return lastRequestTime.getOrDefault(shardId, -1L);
    }

    /**
     * Gets the cache hit rate percentage.
     *
     * @return the cache hit rate (0-100)
     */
    public double getCacheHitRate() {
        long total = cacheHits.get() + cacheMisses.get();
        if (total == 0) {
            return 0;
        }
        return (double) cacheHits.get() / total * 100;
    }

    /**
     * Gets the error rate percentage.
     *
     * @return the error rate (0-100)
     */
    public double getErrorRate() {
        if (totalRequests.get() == 0) {
            return 0;
        }
        return (double) errorCount.get() / totalRequests.get() * 100;
    }

    /**
     * Gets the average requests per second.
     *
     * @return the average requests per second
     */
    public double getAverageRequestsPerSecond() {
        long ageSeconds = getAgeSeconds();
        if (ageSeconds == 0) {
            return 0;
        }
        return (double) totalRequests.get() / ageSeconds;
    }

    /**
     * Gets the stats age in seconds.
     *
     * @return the age in seconds
     */
    public long getAgeSeconds() {
        return java.time.Duration.between(createdAt, Instant.now()).getSeconds();
    }

    /**
     * Resets all statistics.
     */
    public void reset() {
        totalRequests.set(0);
        cacheHits.set(0);
        cacheMisses.set(0);
        routeCalculations.set(0);
        errorCount.set(0);
        requestsByShard.clear();
        errorsByShard.clear();
        lastRequestTime.clear();
    }

    /**
     * Gets a summary of the statistics.
     *
     * @return the summary
     */
    public String getSummary() {
        return String.format(
            "Shard Routing Stats: total=%d, cacheHits=%d, cacheMisses=%d, cacheHitRate=%.2f%%, " +
            "routeCalculations=%d, errors=%d, errorRate=%.2f%%, avgRPS=%.2f",
            getTotalRequests(),
            getCacheHits(),
            getCacheMisses(),
            getCacheHitRate(),
            getRouteCalculations(),
            getErrorCount(),
            getErrorRate(),
            getAverageRequestsPerSecond()
        );
    }

    @Override
    public String toString() {
        return "ShardRoutingStats{" +
            "totalRequests=" + getTotalRequests() +
            ", cacheHitRate=" + getCacheHitRate() +
            ", errorRate=" + getErrorRate() +
            ", avgRPS=" + getAverageRequestsPerSecond() +
            ", ageSeconds=" + getAgeSeconds() +
            '}';
    }
}
