package com.droid.bss.infrastructure.cache.eviction;

import java.util.Map;

/**
 * Interface for cache eviction policies.
 *
 * Determines which entries should be evicted when cache is full or needs cleanup.
 *
 * @since 1.0
 */
public interface EvictionPolicy<K, V> {

    /**
     * Determines which key to evict.
     *
     * @param cacheEntries the cache entries (key -> entry)
     * @param currentSize the current cache size
     * @param maxSize the maximum cache size
     * @return the key to evict (may be null if no eviction is needed)
     */
    K evict(Map<K, CacheEntry<V>> cacheEntries, int currentSize, int maxSize);

    /**
     * Gets the policy name.
     *
     * @return the policy name
     */
    String getName();

    /**
     * Gets the policy description.
     *
     * @return the description
     */
    String getDescription();

    /**
     * Checks if the policy supports concurrent access.
     *
     * @return true if thread-safe
     */
    boolean isThreadSafe();
}
