package com.droid.bss.infrastructure.cache.eviction;

import java.time.Instant;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;

/**
 * Time To Live (TTL) eviction policy.
 *
 * Evicts entries that have exceeded their time-to-live.
 *
 * @since 1.0
 */
public class TTLEvictionPolicy<K, V> implements EvictionPolicy<K, V> {

    private final long ttlMs;

    /**
     * Creates a new TTLEvictionPolicy.
     *
     * @param ttlMs the time-to-live in milliseconds
     */
    public TTLEvictionPolicy(long ttlMs) {
        if (ttlMs <= 0) {
            throw new IllegalArgumentException("TTL must be positive");
        }
        this.ttlMs = ttlMs;
    }

    /**
     * Creates a new TTLEvictionPolicy with TTL from duration.
     *
     * @param ttlMs the time-to-live in milliseconds
     * @return the policy
     */
    public static <K, V> TTLEvictionPolicy<K, V> withTTL(long ttlMs) {
        return new TTLEvictionPolicy<>(ttlMs);
    }

    @Override
    public K evict(Map<K, CacheEntry<V>> cacheEntries, int currentSize, int maxSize) {
        if (cacheEntries == null || cacheEntries.isEmpty()) {
            return null;
        }

        // First, evict expired entries
        Optional<Map.Entry<K, CacheEntry<V>>> expiredEntry = cacheEntries.entrySet().stream()
            .filter(entry -> entry.getValue().isExpired(ttlMs))
            .min(Comparator.comparing(entry -> entry.getValue().getCreatedAt()));

        if (expiredEntry.isPresent()) {
            return expiredEntry.get().getKey();
        }

        // If no expired entries and cache is not full, no eviction needed
        if (currentSize < maxSize) {
            return null;
        }

        // If cache is full and no expired entries, evict the oldest entry
        Optional<Map.Entry<K, CacheEntry<V>>> oldestEntry = cacheEntries.entrySet().stream()
            .min(Comparator.comparing(entry -> entry.getValue().getCreatedAt()));

        return oldestEntry.map(Map.Entry::getKey).orElse(null);
    }

    @Override
    public String getName() {
        return "TTL";
    }

    @Override
    public String getDescription() {
        return "Time To Live - Evicts entries that have exceeded their time-to-live (" + ttlMs + "ms)";
    }

    @Override
    public boolean isThreadSafe() {
        return true;
    }

    /**
     * Gets the TTL in milliseconds.
     *
     * @return the TTL
     */
    public long getTtlMs() {
        return ttlMs;
    }

    @Override
    public String toString() {
        return "TTLEvictionPolicy{name='" + getName() + "', ttlMs=" + ttlMs + '}';
    }
}
