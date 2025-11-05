package com.droid.bss.infrastructure.cache.eviction;

import java.util.Comparator;
import java.util.Map;
import java.util.Optional;

/**
 * Least Frequently Used (LFU) eviction policy.
 *
 * Evicts the entry that has been accessed the least number of times.
 *
 * @since 1.0
 */
public class LFUEvictionPolicy<K, V> implements EvictionPolicy<K, V> {

    @Override
    public K evict(Map<K, CacheEntry<V>> cacheEntries, int currentSize, int maxSize) {
        if (cacheEntries == null || cacheEntries.isEmpty() || currentSize < maxSize) {
            return null;
        }

        // Find the entry with the lowest access count
        // If multiple entries have the same count, evict the one with oldest access time
        Optional<Map.Entry<K, CacheEntry<V>>> leastFrequentEntry = cacheEntries.entrySet().stream()
            .min(Comparator.comparingLong(entry -> entry.getValue().getAccessCount())
                .thenComparing(entry -> entry.getValue().getLastAccessedAt()));

        return leastFrequentEntry.map(Map.Entry::getKey).orElse(null);
    }

    @Override
    public String getName() {
        return "LFU";
    }

    @Override
    public String getDescription() {
        return "Least Frequently Used - Evicts the entry that has been accessed the least number of times";
    }

    @Override
    public boolean isThreadSafe() {
        return true;
    }

    @Override
    public String toString() {
        return "LFUEvictionPolicy{name='" + getName() + "'}";
    }
}
