package com.droid.bss.infrastructure.cache.eviction;

import java.util.Map;

/**
 * Factory for creating cache eviction strategies.
 *
 * @since 1.0
 */
public class CacheEvictionStrategyFactory {

    /**
     * Creates an LRU eviction strategy.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @return the strategy
     */
    public static <K, V> CacheEvictionStrategy<K, V> createLRUStrategy() {
        return new DefaultCacheEvictionStrategy<>(new LRUEvictionPolicy<>());
    }

    /**
     * Creates an LFU eviction strategy.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @return the strategy
     */
    public static <K, V> CacheEvictionStrategy<K, V> createLFUStrategy() {
        return new DefaultCacheEvictionStrategy<>(new LFUEvictionPolicy<>());
    }

    /**
     * Creates a TTL eviction strategy.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param ttlMs the time-to-live in milliseconds
     * @return the strategy
     */
    public static <K, V> CacheEvictionStrategy<K, V> createTLTStrategy(long ttlMs) {
        return new DefaultCacheEvictionStrategy<>(new TTLEvictionPolicy<>(ttlMs));
    }

    /**
     * Creates a TTL eviction strategy.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param ttlMs the time-to-live in milliseconds
     * @return the strategy
     */
    public static <K, V> CacheEvictionStrategy<K, V> createTLTStrategy(int ttlMs) {
        return new DefaultCacheEvictionStrategy<>(new TTLEvictionPolicy<>(ttlMs));
    }

    /**
     * Creates a size-based eviction strategy.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param maxCacheSize the maximum cache size in bytes
     * @return the strategy
     */
    public static <K, V> CacheEvictionStrategy<K, V> createSizeBasedStrategy(long maxCacheSize) {
        return new DefaultCacheEvictionStrategy<>(new SizeBasedEvictionPolicy<>(maxCacheSize));
    }

    /**
     * Creates a combined LRU and TTL eviction strategy.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param ttlMs the time-to-live in milliseconds
     * @return the strategy
     */
    public static <K, V> CacheEvictionStrategy<K, V> createLRUWithTLTStrategy(long ttlMs) {
        EvictionPolicy<K, V> primaryPolicy = new LRUEvictionPolicy<>();
        EvictionPolicy<K, V> ttlPolicy = new TTLEvictionPolicy<>(ttlMs);

        // Create a combined strategy that checks TTL first, then LRU
        return new CombinedEvictionStrategy<>(ttlPolicy, primaryPolicy);
    }

    /**
     * Creates a combined LFU and TTL eviction strategy.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param ttlMs the time-to-live in milliseconds
     * @return the strategy
     */
    public static <K, V> CacheEvictionStrategy<K, V> createLFUWithTLTStrategy(long ttlMs) {
        EvictionPolicy<K, V> primaryPolicy = new LFUEvictionPolicy<>();
        EvictionPolicy<K, V> ttlPolicy = new TTLEvictionPolicy<>(ttlMs);

        // Create a combined strategy that checks TTL first, then LFU
        return new CombinedEvictionStrategy<>(ttlPolicy, primaryPolicy);
    }

    /**
     * Creates a custom eviction strategy.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param policy the eviction policy
     * @return the strategy
     */
    public static <K, V> CacheEvictionStrategy<K, V> createCustomStrategy(EvictionPolicy<K, V> policy) {
        return new DefaultCacheEvictionStrategy<>(policy);
    }

    /**
     * Gets a list of available policy types.
     *
     * @return the list of policy types
     */
    public static String[] getAvailablePolicies() {
        return new String[]{"LRU", "LFU", "TTL", "SIZE", "COMBINED_LRU_TTL", "COMBINED_LFU_TTL"};
    }

    /**
     * Creates a strategy by name.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param policyName the policy name
     * @param parameters the policy parameters
     * @return the strategy
     */
    @SuppressWarnings("unchecked")
    public static <K, V> CacheEvictionStrategy<K, V> createByName(String policyName, Map<String, Object> parameters) {
        if (policyName == null || policyName.isBlank()) {
            throw new IllegalArgumentException("Policy name cannot be null or blank");
        }

        switch (policyName.toUpperCase()) {
            case "LRU":
                return createLRUStrategy();

            case "LFU":
                return createLFUStrategy();

            case "TTL":
                if (parameters == null || !parameters.containsKey("ttlMs")) {
                    throw new IllegalArgumentException("TTL policy requires ttlMs parameter");
                }
                return createTLTStrategy((long) parameters.get("ttlMs"));

            case "SIZE":
                if (parameters == null || !parameters.containsKey("maxCacheSize")) {
                    throw new IllegalArgumentException("SIZE policy requires maxCacheSize parameter");
                }
                return createSizeBasedStrategy((long) parameters.get("maxCacheSize"));

            case "COMBINED_LRU_TTL":
                if (parameters == null || !parameters.containsKey("ttlMs")) {
                    throw new IllegalArgumentException("COMBINED_LRU_TTL policy requires ttlMs parameter");
                }
                return createLRUWithTLTStrategy((long) parameters.get("ttlMs"));

            case "COMBINED_LFU_TTL":
                if (parameters == null || !parameters.containsKey("ttlMs")) {
                    throw new IllegalArgumentException("COMBINED_LFU_TTL policy requires ttlMs parameter");
                }
                return createLFUWithTLTStrategy((long) parameters.get("ttlMs"));

            default:
                throw new IllegalArgumentException("Unknown policy: " + policyName);
        }
    }

    /**
     * Combined eviction strategy that uses multiple policies.
     */
    private static class CombinedEvictionStrategy<K, V> implements CacheEvictionStrategy<K, V> {

        private final EvictionPolicy<K, V> primaryPolicy;
        private final EvictionPolicy<K, V> fallbackPolicy;
        private final EvictionStatistics statistics;

        CombinedEvictionStrategy(EvictionPolicy<K, V> primaryPolicy, EvictionPolicy<K, V> fallbackPolicy) {
            this.primaryPolicy = primaryPolicy;
            this.fallbackPolicy = fallbackPolicy;
            this.statistics = new EvictionStatistics();
        }

        @Override
        public java.util.List<K> evict(java.util.Map<K, CacheEntry<V>> cacheEntries, int currentSize, int maxSize) {
            java.util.List<K> keysToEvict = new java.util.ArrayList<>();

            // Try primary policy first
            K key = primaryPolicy.evict(cacheEntries, currentSize, maxSize);
            if (key != null) {
                keysToEvict.add(key);
                statistics.recordEviction();
            }

            return keysToEvict;
        }

        @Override
        public EvictionPolicy<K, V> getPolicy() {
            return primaryPolicy;
        }

        @Override
        public String getName() {
            return "COMBINED";
        }

        @Override
        public String getDescription() {
            return "Combined - Uses " + primaryPolicy.getName() + " then " + fallbackPolicy.getName();
        }

        @Override
        public java.util.List<K> cleanupExpired(java.util.Map<K, CacheEntry<V>> cacheEntries, long ttlMs) {
            // Return empty list - combined strategy doesn't do direct cleanup
            return java.util.Collections.emptyList();
        }

        @Override
        public java.util.List<K> cleanupIdle(java.util.Map<K, CacheEntry<V>> cacheEntries, long idleThresholdMs) {
            // No-op for combined strategy
            return java.util.Collections.emptyList();
        }

        @Override
        public EvictionStatistics getStatistics() {
            return statistics;
        }
    }
}
