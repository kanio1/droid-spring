package com.droid.bss.infrastructure.cache.eviction;

import java.util.List;
import java.util.Map;

/**
 * Strategy for managing cache eviction.
 *
 * @since 1.0
 */
public interface CacheEvictionStrategy<K, V> {

    /**
     * Evicts entries based on the policy.
     *
     * @param cacheEntries the cache entries
     * @param currentSize the current number of entries
     * @param maxSize the maximum number of entries or cache size
     * @return the list of keys to evict
     */
    List<K> evict(Map<K, CacheEntry<V>> cacheEntries, int currentSize, int maxSize);

    /**
     * Gets the eviction policy in use.
     *
     * @return the policy
     */
    EvictionPolicy<K, V> getPolicy();

    /**
     * Gets the strategy name.
     *
     * @return the name
     */
    String getName();

    /**
     * Gets the strategy description.
     *
     * @return the description
     */
    String getDescription();

    /**
     * Cleans up expired entries.
     *
     * @param cacheEntries the cache entries
     * @param ttlMs the time-to-live in milliseconds
     * @return the list of expired keys
     */
    List<K> cleanupExpired(Map<K, CacheEntry<V>> cacheEntries, long ttlMs);

    /**
     * Cleans up idle entries.
     *
     * @param cacheEntries the cache entries
     * @param idleThresholdMs the idle threshold in milliseconds
     * @return the list of idle keys
     */
    List<K> cleanupIdle(Map<K, CacheEntry<V>> cacheEntries, long idleThresholdMs);

    /**
     * Gets statistics about eviction operations.
     *
     * @return the statistics
     */
    EvictionStatistics getStatistics();
}
