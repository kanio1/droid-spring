package com.droid.bss.infrastructure.cache.advanced;

import com.github.benmanes.caffeine.cache.Cache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * Multi-Layer Cache Manager
 * Manages L1 (Caffeine) and L2 (Redis) caches
 */
@Slf4j
public class MultiLayerCacheManager {

    private final Cache<String, Object> l1Cache;
    private final RedisTemplate<String, Object> l2Cache;
    private final Duration defaultTtl = Duration.ofMinutes(30);
    private final boolean enableWriteThrough = true;
    private final boolean enableCacheAside = true;

    public MultiLayerCacheManager(
            Cache<String, Object> l1Cache,
            RedisTemplate<String, Object> l2Cache) {
        this.l1Cache = l1Cache;
        this.l2Cache = l2Cache;
    }

    /**
     * Get value from cache (L1 first, then L2)
     */
    public <T> Optional<T> get(String key, Class<T> type) {
        // Try L1 cache
        Object value = l1Cache.getIfPresent(key);
        if (value != null) {
            log.trace("Cache hit (L1): {}", key);
            return Optional.of(type.cast(value));
        }

        // Try L2 cache
        value = l2Cache.opsForValue().get(key);
        if (value != null) {
            log.trace("Cache hit (L2): {}", key);
            // Populate L1 cache
            l1Cache.put(key, value);
            return Optional.of(type.cast(value));
        }

        log.trace("Cache miss: {}", key);
        return Optional.empty();
    }

    /**
     * Put value in both caches
     */
    public void put(String key, Object value) {
        put(key, value, defaultTtl);
    }

    /**
     * Put value in both caches with TTL
     */
    public void put(String key, Object value, Duration ttl) {
        // Put in L1 cache
        l1Cache.put(key, value);

        // Put in L2 cache
        l2Cache.opsForValue().set(key, value, ttl.toMillis(), TimeUnit.MILLISECONDS);

        log.trace("Cache put: {} (TTL: {}ms)", key, ttl.toMillis());
    }

    /**
     * Remove value from both caches
     */
    public void evict(String key) {
        l1Cache.invalidate(key);
        l2Cache.delete(key);
        log.trace("Cache evict: {}", key);
    }

    /**
     * Clear all caches
     */
    public void clear() {
        l1Cache.invalidateAll();
        l2Cache.getConnectionFactory().getConnection().flushDb();
        log.info("Cache cleared");
    }

    /**
     * Get or load pattern (Cache-Aside)
     */
    public <T> T getOrLoad(String key, Function<String, T> loader, Duration ttl, Class<T> type) {
        // Try cache first
        Optional<T> cached = get(key, type);
        if (cached.isPresent()) {
            return cached.get();
        }

        // Load from source
        log.trace("Cache miss, loading: {}", key);
        T value = loader.apply(key);

        // Store in cache
        if (value != null) {
            put(key, value, ttl);
        }

        return value;
    }

    /**
     * Batch evict by pattern
     */
    public void evictPattern(String pattern) {
        // L1 cache
        Set<String> l1Keys = l1Cache.asMap().keySet().stream()
            .filter(k -> k.contains(pattern.replace("*", "")))
            .collect(HashSet::new, HashSet::add, HashSet::addAll);
        l1Keys.forEach(l1Cache::invalidate);

        // L2 cache
        Set<String> l2Keys = l2Cache.keys("*" + pattern + "*");
        if (l2Keys != null && !l2Keys.isEmpty()) {
            l2Cache.delete(l2Keys);
        }

        log.info("Evicted {} keys matching pattern: {}", l1Keys.size() + l2Keys.size(), pattern);
    }

    /**
     * Get cache statistics
     */
    public CacheStatistics getStatistics() {
        var l1Stats = l1Cache.stats();
        var l1Size = l1Cache.estimatedSize();

        try {
            var l2Info = l2Cache.getConnectionFactory().getConnection().info("memory");
            var l2Size = l2Cache.keys("*").size();

            return new CacheStatistics(
                l1Size,
                l1Stats.hitCount(),
                l1Stats.missCount(),
                l1Stats.evictionCount(),
                l2Size,
                extractUsedMemory(l2Info)
            );
        } catch (Exception e) {
            log.warn("Failed to get L2 statistics", e);
            return new CacheStatistics(l1Size, l1Stats.hitCount(), l1Stats.missCount(), l1Stats.evictionCount(), 0, 0);
        }
    }

    private long extractUsedMemory(String info) {
        // Simple parsing of Redis INFO memory
        String[] lines = info.split("\r\n");
        for (String line : lines) {
            if (line.startsWith("used_memory_human:")) {
                return parseMemory(line.substring("used_memory_human:".length()).trim());
            }
        }
        return 0;
    }

    private long parseMemory(String value) {
        // Parse values like "1.2M", "500K", etc.
        if (value.endsWith("M")) {
            return (long) (Double.parseDouble(value.substring(0, value.length() - 1)) * 1024 * 1024);
        } else if (value.endsWith("K")) {
            return (long) (Double.parseDouble(value.substring(0, value.length() - 1)) * 1024);
        }
        return Long.parseLong(value);
    }

    /**
     * Check if key exists
     */
    public boolean hasKey(String key) {
        if (l1Cache.getIfPresent(key) != null) {
            return true;
        }
        return Boolean.TRUE.equals(l2Cache.hasKey(key));
    }

    /**
     * Get TTL of a key
     */
    public Duration getTtl(String key) {
        Long ttl = l2Cache.getExpire(key);
        return ttl != null && ttl > 0 ? Duration.ofMillis(ttl) : Duration.ofSeconds(-1);
    }

    /**
     * Extend TTL of a key
     */
    public void extendTtl(String key, Duration ttl) {
        l2Cache.expire(key, ttl.toMillis(), TimeUnit.MILLISECONDS);
    }

    /**
     * Cache statistics
     */
    public static class CacheStatistics {
        private final long l1Size;
        private final long l1Hits;
        private final long l1Misses;
        private final long l1Evictions;
        private final long l2Size;
        private final long l2MemoryUsage;

        public CacheStatistics(
                long l1Size,
                long l1Hits,
                long l1Misses,
                long l1Evictions,
                long l2Size,
                long l2MemoryUsage) {
            this.l1Size = l1Size;
            this.l1Hits = l1Hits;
            this.l1Misses = l1Misses;
            this.l1Evictions = l1Evictions;
            this.l2Size = l2Size;
            this.l2MemoryUsage = l2MemoryUsage;
        }

        public long getL1Size() { return l1Size; }
        public long getL1Hits() { return l1Hits; }
        public long getL1Misses() { return l1Misses; }
        public long getL1Evictions() { return l1Evictions; }
        public long getL2Size() { return l2Size; }
        public long getL2MemoryUsage() { return l2MemoryUsage; }

        public double getL1HitRate() {
            long total = l1Hits + l1Misses;
            return total > 0 ? (double) l1Hits / total : 0.0;
        }

        @Override
        public String toString() {
            return String.format(
                "L1: size=%d, hits=%d, misses=%d, evictions=%d, hitRate=%.2f%% | L2: size=%d, memory=%dMB",
                l1Size, l1Hits, l1Misses, l1Evictions, getL1HitRate() * 100, l2Size, l2MemoryUsage / (1024 * 1024)
            );
        }
    }
}
