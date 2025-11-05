package com.droid.bss.infrastructure.cache.eviction;

import java.util.Comparator;
import java.util.Map;
import java.util.Optional;

/**
 * Least Recently Used (LRU) eviction policy.
 *
 * Evicts the entry that hasn't been accessed for the longest time.
 *
 * @since 1.0
 */
public class LRUEvictionPolicy<K, V> implements EvictionPolicy<K, V> {

    @Override
    public K evict(Map<K, CacheEntry<V>> cacheEntries, int currentSize, int maxSize) {
        if (cacheEntries == null || cacheEntries.isEmpty() || currentSize < maxSize) {
            return null;
        }

        // Find the entry with the oldest last accessed time
        Optional<Map.Entry<K, CacheEntry<V>>> oldestEntry = cacheEntries.entrySet().stream()
            .min(Comparator.comparing(entry -> entry.getValue().getLastAccessedAt()));

        return oldestEntry.map(Map.Entry::getKey).orElse(null);
    }

    @Override
    public String getName() {
        return "LRU";
    }

    @Override
    public String getDescription() {
        return "Least Recently Used - Evicts the entry that hasn't been accessed for the longest time";
    }

    @Override
    public boolean isThreadSafe() {
        return true;
    }

    @Override
    public String toString() {
        return "LRUEvictionPolicy{name='" + getName() + "'}";
    }
}
