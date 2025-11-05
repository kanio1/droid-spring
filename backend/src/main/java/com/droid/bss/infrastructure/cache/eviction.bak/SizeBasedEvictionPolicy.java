package com.droid.bss.infrastructure.cache.eviction;

import java.util.Comparator;
import java.util.Map;
import java.util.Optional;

/**
 * Size-based eviction policy.
 *
 * Evicts entries based on their size to keep total cache size under the maximum.
 *
 * @since 1.0
 */
public class SizeBasedEvictionPolicy<K, V> implements EvictionPolicy<K, V> {

    private final long maxCacheSize;

    /**
     * Creates a new SizeBasedEvictionPolicy.
     *
     * @param maxCacheSize the maximum cache size in bytes
     */
    public SizeBasedEvictionPolicy(long maxCacheSize) {
        if (maxCacheSize <= 0) {
            throw new IllegalArgumentException("Max cache size must be positive");
        }
        this.maxCacheSize = maxCacheSize;
    }

    /**
     * Creates a new SizeBasedEvictionPolicy with max size.
     *
     * @param maxCacheSize the maximum cache size in bytes
     * @return the policy
     */
    public static <K, V> SizeBasedEvictionPolicy<K, V> withMaxSize(long maxCacheSize) {
        return new SizeBasedEvictionPolicy<>(maxCacheSize);
    }

    @Override
    public K evict(Map<K, CacheEntry<V>> cacheEntries, int currentSize, int maxSize) {
        if (cacheEntries == null || cacheEntries.isEmpty()) {
            return null;
        }

        // Calculate current total size
        long currentTotalSize = cacheEntries.values().stream()
            .mapToLong(CacheEntry::getSize)
            .sum();

        // If cache size is within limit, no eviction needed
        if (currentTotalSize <= maxCacheSize) {
            return null;
        }

        // Find the largest entry to evict (to free up the most space)
        Optional<Map.Entry<K, CacheEntry<V>>> largestEntry = cacheEntries.entrySet().stream()
            .max(Comparator.comparingLong(entry -> entry.getValue().getSize()));

        return largestEntry.map(Map.Entry::getKey).orElse(null);
    }

    /**
     * Checks if adding an entry would exceed the cache size limit.
     *
     * @param cacheEntries the cache entries
     * @param newEntrySize the size of the new entry
     * @return true if eviction is needed
     */
    public boolean needsEviction(Map<K, CacheEntry<V>> cacheEntries, long newEntrySize) {
        if (cacheEntries == null) {
            return false;
        }

        long currentTotalSize = cacheEntries.values().stream()
            .mapToLong(CacheEntry::getSize)
            .sum();

        return (currentTotalSize + newEntrySize) > maxCacheSize;
    }

    /**
     * Calculates how much space needs to be freed.
     *
     * @param cacheEntries the cache entries
     * @param newEntrySize the size of the new entry
     * @return the space that needs to be freed
     */
    public long calculateSpaceToFree(Map<K, CacheEntry<V>> cacheEntries, long newEntrySize) {
        if (cacheEntries == null) {
            return 0;
        }

        long currentTotalSize = cacheEntries.values().stream()
            .mapToLong(CacheEntry::getSize)
            .sum();

        long totalWithNewEntry = currentTotalSize + newEntrySize;

        if (totalWithNewEntry <= maxCacheSize) {
            return 0;
        }

        return totalWithNewEntry - maxCacheSize;
    }

    @Override
    public String getName() {
        return "SIZE";
    }

    @Override
    public String getDescription() {
        return "Size-Based - Evicts entries based on their size to keep total cache size under " + maxCacheSize + " bytes";
    }

    @Override
    public boolean isThreadSafe() {
        return true;
    }

    /**
     * Gets the maximum cache size.
     *
     * @return the maximum cache size
     */
    public long getMaxCacheSize() {
        return maxCacheSize;
    }

    @Override
    public String toString() {
        return "SizeBasedEvictionPolicy{name='" + getName() + "', maxCacheSize=" + maxCacheSize + '}';
    }
}
