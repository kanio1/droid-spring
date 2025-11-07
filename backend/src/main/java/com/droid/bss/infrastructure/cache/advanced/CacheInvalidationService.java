package com.droid.bss.infrastructure.cache.advanced;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Cache Invalidation Service
 * Manages cache invalidation strategies
 */
@Slf4j
@Service
public class CacheInvalidationService {

    private final MultiLayerCacheManager cacheManager;
    private final CacheKeyGenerator keyGenerator;
    private final Map<String, Set<String>> cacheDependencies = new ConcurrentHashMap<>();
    private final Map<String, Long> lastInvalidationTime = new ConcurrentHashMap<>();
    private final AtomicInteger totalInvalidations = new AtomicInteger(0);
    private final AtomicInteger totalPatternInvalidations = new AtomicInteger(0);

    public CacheInvalidationService(
            MultiLayerCacheManager cacheManager,
            CacheKeyGenerator keyGenerator) {
        this.cacheManager = cacheManager;
        this.keyGenerator = keyGenerator;
    }

    /**
     * Register cache dependencies
     */
    public void registerDependency(String entityId, Set<String> relatedKeys) {
        cacheDependencies.put(entityId, relatedKeys);
        log.trace("Registered dependency: {} -> {}", entityId, relatedKeys);
    }

    /**
     * Invalidate cache by key
     */
    public void invalidate(String key) {
        cacheManager.evict(key);
        lastInvalidationTime.put(key, System.currentTimeMillis());
        totalInvalidations.incrementAndGet();
        log.trace("Invalidated cache key: {}", key);
    }

    /**
     * Invalidate by pattern
     */
    public void invalidatePattern(String pattern) {
        cacheManager.evictPattern(pattern);
        totalPatternInvalidations.incrementAndGet();
        lastInvalidationTime.put("pattern:" + pattern, System.currentTimeMillis());
        log.info("Invalidated cache pattern: {}", pattern);
    }

    /**
     * Invalidate when entity changes
     */
    public void invalidateEntity(String entityType, String entityId) {
        String entityKey = keyGenerator.generateEntityKey(entityType, entityId);
        invalidate(entityKey);

        // Invalidate related keys
        Set<String> relatedKeys = cacheDependencies.get(entityId);
        if (relatedKeys != null) {
            for (String key : relatedKeys) {
                invalidate(key);
            }
        }

        // Invalidate list views
        String listPattern = keyGenerator.generatePattern(entityType, "list");
        invalidatePattern(listPattern);

        // Invalidate aggregates
        String aggregatePattern = keyGenerator.generatePattern(entityType, "aggregate");
        invalidatePattern(aggregatePattern);

        log.info("Invalidated entity cache: {}:{}", entityType, entityId);
    }

    /**
     * Invalidate all related to a customer
     */
    public void invalidateCustomerData(String customerId) {
        // Invalidate customer entity
        invalidateEntity("customer", customerId);

        // Invalidate customer orders
        invalidatePattern("order:customer:" + customerId + ":*");

        // Invalidate customer invoices
        invalidatePattern("invoice:customer:" + customerId + ":*");

        // Invalidate customer payments
        invalidatePattern("payment:customer:" + customerId + ":*");

        // Invalidate customer subscriptions
        invalidatePattern("subscription:customer:" + customerId + ":*");

        log.info("Invalidated all customer data for: {}", customerId);
    }

    /**
     * Invalidate all related to a product
     */
    public void invalidateProductData(String productId) {
        // Invalidate product entity
        invalidateEntity("product", productId);

        // Invalidate product in orders
        invalidatePattern("order:*:product:" + productId);

        // Invalidate inventory
        invalidatePattern("inventory:product:" + productId);

        // Invalidate recommendations
        invalidatePattern("recommendation:product:" + productId);

        log.info("Invalidated all product data for: {}", productId);
    }

    /**
     * Batch invalidate
     */
    public void batchInvalidate(Collection<String> keys) {
        for (String key : keys) {
            invalidate(key);
        }
        log.info("Batch invalidated {} cache keys", keys.size());
    }

    /**
     * Get invalidation statistics
     */
    public CacheInvalidationStatistics getStatistics() {
        return new CacheInvalidationStatistics(
            totalInvalidations.get(),
            totalPatternInvalidations.get(),
            cacheDependencies.size(),
            lastInvalidationTime.size()
        );
    }

    /**
     * Get last invalidation time for a key
     */
    public Optional<Long> getLastInvalidationTime(String key) {
        return Optional.ofNullable(lastInvalidationTime.get(key));
    }

    /**
     * Check if a key was recently invalidated
     */
    public boolean wasRecentlyInvalidated(String key, Duration threshold) {
        return getLastInvalidationTime(key)
            .map(lastTime -> System.currentTimeMillis() - lastTime < threshold.toMillis())
            .orElse(false);
    }

    /**
     * Clear all invalidation data
     */
    public void clearInvalidationData() {
        cacheDependencies.clear();
        lastInvalidationTime.clear();
        totalInvalidations.set(0);
        totalPatternInvalidations.set(0);
        log.info("Cleared cache invalidation data");
    }

    /**
     * Invalidation statistics
     */
    public static class CacheInvalidationStatistics {
        private final int totalKeyInvalidations;
        private final int totalPatternInvalidations;
        private final int registeredDependencies;
        private final int trackedKeys;

        public CacheInvalidationStatistics(
                int totalKeyInvalidations,
                int totalPatternInvalidations,
                int registeredDependencies,
                int trackedKeys) {
            this.totalKeyInvalidations = totalKeyInvalidations;
            this.totalPatternInvalidations = totalPatternInvalidations;
            this.registeredDependencies = registeredDependencies;
            this.trackedKeys = trackedKeys;
        }

        public int getTotalKeyInvalidations() { return totalKeyInvalidations; }
        public int getTotalPatternInvalidations() { return totalPatternInvalidations; }
        public int getRegisteredDependencies() { return registeredDependencies; }
        public int getTrackedKeys() { return trackedKeys; }

        @Override
        public String toString() {
            return String.format(
                "Invalidations: %d key, %d pattern | Dependencies: %d | Tracked keys: %d",
                totalKeyInvalidations, totalPatternInvalidations, registeredDependencies, trackedKeys
            );
        }
    }
}
