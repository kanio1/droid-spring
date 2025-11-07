package com.droid.bss.infrastructure.cache.advanced;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Cache Statistics Service
 * Collects and reports cache performance metrics
 */
@Slf4j
@Service
public class CacheStatisticsService {

    private final MultiLayerCacheManager cacheManager;
    private final Map<String, CacheAccessRecord> accessHistory = new ConcurrentHashMap<>();
    private final AtomicLong totalCacheHits = new AtomicLong(0);
    private final AtomicLong totalCacheMisses = new AtomicLong(0);
    private final AtomicLong totalCachePuts = new AtomicLong(0);
    private final AtomicLong totalCacheEvictions = new AtomicLong(0);

    public CacheStatisticsService(MultiLayerCacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    /**
     * Record cache access
     */
    public void recordCacheHit(String key) {
        totalCacheHits.incrementAndGet();
        updateAccessRecord(key, true);
        log.trace("Cache hit: {}", key);
    }

    /**
     * Record cache miss
     */
    public void recordCacheMiss(String key) {
        totalCacheMisses.incrementAndGet();
        updateAccessRecord(key, false);
        log.trace("Cache miss: {}", key);
    }

    /**
     * Record cache put
     */
    public void recordCachePut(String key) {
        totalCachePuts.incrementAndGet();
        log.trace("Cache put: {}", key);
    }

    /**
     * Record cache eviction
     */
    public void recordCacheEviction(String key) {
        totalCacheEvictions.incrementAndGet();
        log.trace("Cache eviction: {}", key);
    }

    /**
     * Get comprehensive cache statistics
     */
    public CachePerformanceStatistics getStatistics() {
        MultiLayerCacheManager.CacheStatistics basicStats = cacheManager.getStatistics();

        long hits = totalCacheHits.get();
        long misses = totalCacheMisses.get();
        long total = hits + misses;

        return new CachePerformanceStatistics(
            basicStats,
            hits,
            misses,
            total,
            totalCachePuts.get(),
            totalCacheEvictions.get(),
            calculateHitRate(hits, misses),
            calculateMissRate(hits, misses),
            accessHistory.size(),
            getTopAccessedKeys(10),
            LocalDateTime.now()
        );
    }

    /**
     * Get cache statistics for a specific namespace
     */
    public Map<String, Object> getNamespaceStatistics(String namespace) {
        Map<String, Object> stats = new HashMap<>();
        List<CacheAccessRecord> namespaceRecords = accessHistory.entrySet().stream()
            .filter(entry -> entry.getKey().startsWith(namespace))
            .map(Map.Entry::getValue)
            .toList();

        if (namespaceRecords.isEmpty()) {
            return stats;
        }

        long hits = namespaceRecords.stream()
            .mapToLong(CacheAccessRecord::getHitCount)
            .sum();
        long misses = namespaceRecords.stream()
            .mapToLong(CacheAccessRecord::getMissCount)
            .sum();

        stats.put("namespace", namespace);
        stats.put("totalKeys", namespaceRecords.size());
        stats.put("totalHits", hits);
        stats.put("totalMisses", misses);
        stats.put("hitRate", calculateHitRate(hits, misses));
        stats.put("mostAccessedKey", namespaceRecords.stream()
            .max(Comparator.comparing(CacheAccessRecord::getAccessCount))
            .map(CacheAccessRecord::getKey)
            .orElse(null));

        return stats;
    }

    /**
     * Get hot keys (most accessed)
     */
    public List<String> getHotKeys(int limit) {
        return accessHistory.values().stream()
            .sorted(Comparator.comparing(CacheAccessRecord::getAccessCount).reversed())
            .limit(limit)
            .map(CacheAccessRecord::getKey)
            .toList();
    }

    /**
     * Get cold keys (least accessed)
     */
    public List<String> getColdKeys(int limit) {
        return accessHistory.values().stream()
            .sorted(Comparator.comparing(CacheAccessRecord::getAccessCount))
            .limit(limit)
            .map(CacheAccessRecord::getKey)
            .toList();
    }

    /**
     * Get cache efficiency score (0-100)
     */
    public double getCacheEfficiencyScore() {
        long hits = totalCacheHits.get();
        long misses = totalCacheMisses.get();
        long total = hits + misses;

        if (total == 0) {
            return 0.0;
        }

        double hitRate = (double) hits / total;
        return hitRate * 100.0;
    }

    /**
     * Check if cache is performing well
     */
    public boolean isCacheHealthy() {
        double efficiency = getCacheEfficiencyScore();
        return efficiency >= 80.0;
    }

    /**
     * Reset statistics
     */
    public void resetStatistics() {
        totalCacheHits.set(0);
        totalCacheMisses.set(0);
        totalCachePuts.set(0);
        totalCacheEvictions.set(0);
        accessHistory.clear();
        log.info("Cache statistics reset");
    }

    private void updateAccessRecord(String key, boolean hit) {
        CacheAccessRecord record = accessHistory.computeIfAbsent(key, k -> {
            CacheAccessRecord newRecord = new CacheAccessRecord(k);
            accessHistory.put(k, newRecord);
            return newRecord;
        });

        record.recordAccess(hit);
    }

    private double calculateHitRate(long hits, long misses) {
        long total = hits + misses;
        return total > 0 ? ((double) hits / total) * 100.0 : 0.0;
    }

    private double calculateMissRate(long hits, long misses) {
        long total = hits + misses;
        return total > 0 ? ((double) misses / total) * 100.0 : 0.0;
    }

    private List<String> getTopAccessedKeys(int limit) {
        return accessHistory.values().stream()
            .sorted(Comparator.comparing(CacheAccessRecord::getAccessCount).reversed())
            .limit(limit)
            .map(CacheAccessRecord::getKey)
            .toList();
    }

    /**
     * Cache access record
     */
    public static class CacheAccessRecord {
        private final String key;
        private long hitCount = 0;
        private long missCount = 0;
        private long lastAccessTime;
        private long accessCount = 0;

        public CacheAccessRecord(String key) {
            this.key = key;
        }

        public void recordAccess(boolean hit) {
            if (hit) {
                hitCount++;
            } else {
                missCount++;
            }
            accessCount++;
            lastAccessTime = System.currentTimeMillis();
        }

        public String getKey() { return key; }
        public long getHitCount() { return hitCount; }
        public long getMissCount() { return missCount; }
        public long getLastAccessTime() { return lastAccessTime; }
        public long getAccessCount() { return accessCount; }
    }

    /**
     * Performance statistics
     */
    public static class CachePerformanceStatistics {
        private final MultiLayerCacheManager.CacheStatistics basicStats;
        private final long totalHits;
        private final long totalMisses;
        private final long totalRequests;
        private final long totalPuts;
        private final long totalEvictions;
        private final double hitRate;
        private final double missRate;
        private final int trackedKeys;
        private final List<String> topAccessedKeys;
        private final LocalDateTime timestamp;

        public CachePerformanceStatistics(
                MultiLayerCacheManager.CacheStatistics basicStats,
                long totalHits,
                long totalMisses,
                long totalRequests,
                long totalPuts,
                long totalEvictions,
                double hitRate,
                double missRate,
                int trackedKeys,
                List<String> topAccessedKeys,
                LocalDateTime timestamp) {
            this.basicStats = basicStats;
            this.totalHits = totalHits;
            this.totalMisses = totalMisses;
            this.totalRequests = totalRequests;
            this.totalPuts = totalPuts;
            this.totalEvictions = totalEvictions;
            this.hitRate = hitRate;
            this.missRate = missRate;
            this.trackedKeys = trackedKeys;
            this.topAccessedKeys = topAccessedKeys;
            this.timestamp = timestamp;
        }

        public MultiLayerCacheManager.CacheStatistics getBasicStats() { return basicStats; }
        public long getTotalHits() { return totalHits; }
        public long getTotalMisses() { return totalMisses; }
        public long getTotalRequests() { return totalRequests; }
        public long getTotalPuts() { return totalPuts; }
        public long getTotalEvictions() { return totalEvictions; }
        public double getHitRate() { return hitRate; }
        public double getMissRate() { return missRate; }
        public int getTrackedKeys() { return trackedKeys; }
        public List<String> getTopAccessedKeys() { return topAccessedKeys; }
        public LocalDateTime getTimestamp() { return timestamp; }

        @Override
        public String toString() {
            return String.format(
                "Cache Performance: HitRate=%.2f%%, MissRate=%.2f%%, " +
                "Hits=%d, Misses=%d, Puts=%d, Evictions=%d, " +
                "TrackedKeys=%d, BasicStats=[%s]",
                hitRate, missRate, totalHits, totalMisses, totalPuts,
                totalEvictions, trackedKeys, basicStats.toString()
            );
        }
    }
}
