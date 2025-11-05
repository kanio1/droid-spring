package com.droid.bss.infrastructure.cache.eviction;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Default implementation of CacheEvictionStrategy.
 *
 * @since 1.0
 */
public class DefaultCacheEvictionStrategy<K, V> implements CacheEvictionStrategy<K, V> {

    private final EvictionPolicy<K, V> policy;
    private final EvictionStatistics statistics;

    /**
     * Creates a new DefaultCacheEvictionStrategy.
     *
     * @param policy the eviction policy
     */
    public DefaultCacheEvictionStrategy(EvictionPolicy<K, V> policy) {
        if (policy == null) {
            throw new IllegalArgumentException("Eviction policy cannot be null");
        }
        this.policy = policy;
        this.statistics = new EvictionStatistics();
    }

    @Override
    public List<K> evict(Map<K, CacheEntry<V>> cacheEntries, int currentSize, int maxSize) {
        if (cacheEntries == null || cacheEntries.isEmpty()) {
            return Collections.emptyList();
        }

        K keyToEvict = policy.evict(cacheEntries, currentSize, maxSize);

        if (keyToEvict != null) {
            statistics.recordPolicyEviction();
            return Collections.singletonList(keyToEvict);
        }

        return Collections.emptyList();
    }

    @Override
    public EvictionPolicy<K, V> getPolicy() {
        return policy;
    }

    @Override
    public String getName() {
        return policy.getName();
    }

    @Override
    public String getDescription() {
        return policy.getDescription();
    }

    @Override
    public List<K> cleanupExpired(Map<K, CacheEntry<V>> cacheEntries, long ttlMs) {
        if (cacheEntries == null || cacheEntries.isEmpty() || ttlMs <= 0) {
            return Collections.emptyList();
        }

        List<K> expiredKeys = cacheEntries.entrySet().stream()
            .filter(entry -> entry.getValue().isExpired(ttlMs))
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());

        if (!expiredKeys.isEmpty()) {
            expiredKeys.forEach(key -> cacheEntries.remove(key));
            statistics.recordExpiredEviction();
        }

        return expiredKeys;
    }

    @Override
    public List<K> cleanupIdle(Map<K, CacheEntry<V>> cacheEntries, long idleThresholdMs) {
        if (cacheEntries == null || cacheEntries.isEmpty() || idleThresholdMs <= 0) {
            return Collections.emptyList();
        }

        List<K> idleKeys = cacheEntries.entrySet().stream()
            .filter(entry -> entry.getValue().isIdle(idleThresholdMs))
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());

        if (!idleKeys.isEmpty()) {
            idleKeys.forEach(key -> cacheEntries.remove(key));
            statistics.recordIdleEviction();
        }

        return idleKeys;
    }

    @Override
    public EvictionStatistics getStatistics() {
        return statistics;
    }
}
