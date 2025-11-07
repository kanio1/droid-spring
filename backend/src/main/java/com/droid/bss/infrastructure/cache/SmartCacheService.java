package com.droid.bss.infrastructure.cache;

import com.droid.bss.application.service.PerformanceCacheService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Smart Cache Service with intelligent features:
 * - Pre-warming for VIP customers
 * - Smart invalidation based on events
 * - Cache metrics and monitoring
 * - Custom TTL per entity type
 */
@Service
public class SmartCacheService {

    private final PerformanceCacheService cacheService;
    private final CacheManager cacheManager;
    private final Executor asyncExecutor;

    // Cache configuration for different entity types
    private static final Duration CUSTOMER_TTL = Duration.ofMinutes(10);
    private static final Duration INVOICE_TTL = Duration.ofMinutes(5);
    private static final Duration PAYMENT_TTL = Duration.ofMinutes(3);
    private static final Duration DASHBOARD_TTL = Duration.ofMinutes(2);

    // VIP customer patterns
    private static final Set<String> VIP_CUSTOMER_PREFIXES = Set.of("vip-", "enterprise-", "premium-");

    public SmartCacheService(
            PerformanceCacheService cacheService,
            CacheManager cacheManager,
            @Qualifier("taskExecutor") Executor asyncExecutor) {
        this.cacheService = cacheService;
        this.cacheManager = cacheManager;
        this.asyncExecutor = asyncExecutor;
    }

    /**
     * Pre-warm cache for VIP customers
     * Loads frequently accessed data before it's requested
     */
    public void prewarmCustomerCache(String customerId) {
        if (isVipCustomer(customerId)) {
            CompletableFuture.runAsync(() -> {
                // Pre-warm customer data
                warmCache("customer:" + customerId, () -> {
                    // This would be replaced with actual data loading
                    return "customer_data_for_" + customerId;
                }, CUSTOMER_TTL);

                // Pre-warm customer dashboard
                warmCache("dashboard:customer:" + customerId, () -> {
                    return "dashboard_data_for_" + customerId;
                }, DASHBOARD_TTL);

                // Pre-warm customer invoices
                warmCache("invoices:customer:" + customerId, () -> {
                    return "invoices_data_for_" + customerId;
                }, INVOICE_TTL);
            }, asyncExecutor);
        }
    }

    /**
     * Smart cache invalidation based on entity changes
     * Automatically evicts related cache entries when data is updated
     */
    public void onCustomerUpdated(String customerId) {
        // Evict customer cache
        evictPattern("customer:" + customerId);

        // Evict customer dashboard
        evictPattern("dashboard:customer:" + customerId);

        // Evict customer invoices
        evictPattern("invoices:customer:" + customerId);

        // Evict search results that might include this customer
        evictPattern("search:customers:*");

        // Re-warm if VIP customer
        if (isVipCustomer(customerId)) {
            prewarmCustomerCache(customerId);
        }
    }

    /**
     * Smart cache invalidation for invoice changes
     */
    public void onInvoiceUpdated(String customerId, String invoiceId) {
        // Evict customer invoices cache
        evictPattern("invoices:customer:" + customerId);
        evictPattern("invoice:" + invoiceId);

        // Evict customer dashboard
        evictPattern("dashboard:customer:" + customerId);

        // Re-warm for VIP customers
        if (isVipCustomer(customerId)) {
            prewarmCustomerCache(customerId);
        }
    }

    /**
     * Smart cache invalidation for payment changes
     */
    public void onPaymentUpdated(String customerId) {
        // Evict payment cache
        evictPattern("payments:customer:" + customerId);

        // Evict customer dashboard
        evictPattern("dashboard:customer:" + customerId);

        // Re-warm for VIP customers
        if (isVipCustomer(customerId)) {
            prewarmCustomerCache(customerId);
        }
    }

    /**
     * Cache with custom TTL based on entity type
     */
    public <T> void cacheWithCustomTtl(String cacheName, String key, T value, EntityType entityType) {
        Duration ttl = getTtlForEntityType(entityType);
        cacheService.setWithCustomTtl(cacheName + ":" + key, value, ttl.getSeconds(), java.util.concurrent.TimeUnit.SECONDS);
    }

    /**
     * Get cached value with custom TTL
     */
    public <T> T getFromCache(String cacheName, String key, Class<T> type) {
        return cacheService.get(cacheName + ":" + key, type);
    }

    /**
     * Get cache statistics
     */
    public CacheStatistics getCacheStatistics() {
        return CacheStatistics.builder()
                .cacheManager(cacheManager.getClass().getSimpleName())
                .cacheNames(cacheManager.getCacheNames())
                .build();
    }

    /**
     * Warm up multiple cache entries
     */
    public void batchPrewarm(List<CacheEntry> entries) {
        entries.forEach(entry -> {
            CompletableFuture.runAsync(() -> {
                warmCache(entry.getKey(), entry.getLoader(), entry.getTtl());
            }, asyncExecutor);
        });
    }

    /**
     * Invalidate cache by pattern (e.g., "customer:*")
     */
    private void evictPattern(String pattern) {
        cacheService.evictPattern(pattern);
    }

    /**
     * Check if customer is VIP (has special caching needs)
     */
    private boolean isVipCustomer(String customerId) {
        return VIP_CUSTOMER_PREFIXES.stream()
                .anyMatch(customerId::startsWith);
    }

    /**
     * Get TTL for specific entity type
     */
    private Duration getTtlForEntityType(EntityType entityType) {
        return switch (entityType) {
            case CUSTOMER -> CUSTOMER_TTL;
            case INVOICE -> INVOICE_TTL;
            case PAYMENT -> PAYMENT_TTL;
            case DASHBOARD -> DASHBOARD_TTL;
        };
    }

    /**
     * Internal method to warm cache
     */
    private <T> void warmCache(String key, CacheLoader<T> loader, Duration ttl) {
        try {
            T data = loader.load();
            if (data != null) {
                cacheService.setWithCustomTtl(key, data, ttl.getSeconds(), java.util.concurrent.TimeUnit.SECONDS);
            }
        } catch (Exception e) {
            // Log error but don't fail the pre-warming process
            System.err.println("Failed to warm cache for key: " + key + ", error: " + e.getMessage());
        }
    }

    /**
     * Cache loader functional interface
     */
    @FunctionalInterface
    public interface CacheLoader<T> {
        T load();
    }

    /**
     * Entity types for cache configuration
     */
    public enum EntityType {
        CUSTOMER,
        INVOICE,
        PAYMENT,
        DASHBOARD
    }

    /**
     * Cache entry for batch operations
     */
    public static class CacheEntry {
        private final String key;
        private final CacheLoader<Object> loader;
        private final Duration ttl;

        public CacheEntry(String key, CacheLoader<Object> loader, Duration ttl) {
            this.key = key;
            this.loader = loader;
            this.ttl = ttl;
        }

        public String getKey() { return key; }
        public CacheLoader<Object> getLoader() { return loader; }
        public Duration getTtl() { return ttl; }
    }
}
