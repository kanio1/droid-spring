package com.droid.bss.infrastructure.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Cache Eviction Service
 *
 * Handles cache invalidation when data changes via events
 */
@Service
public class CacheEvictionService {

    private static final Logger log = LoggerFactory.getLogger(CacheEvictionService.class);
    private final CacheManager cacheManager;

    public CacheEvictionService(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    /**
     * Evict customer cache entries
     */
    @CacheEvict(value = "customers", key = "#customerId")
    public void evictCustomerCache(String customerId) {
        log.debug("Evicted customer cache for ID: {}", customerId);
    }

    /**
     * Evict all customer list cache entries
     */
    public void evictAllCustomerListCaches() {
        if (cacheManager.getCache("customers") != null) {
            cacheManager.getCache("customers").clear();
            log.debug("Evicted all customer list caches");
        }
    }

    /**
     * Evict order cache entries
     */
    @CacheEvict(value = "orders", key = "#orderId")
    public void evictOrderCache(UUID orderId) {
        log.debug("Evicted order cache for ID: {}", orderId);
    }

    /**
     * Evict all order list cache entries
     */
    public void evictAllOrderCaches() {
        if (cacheManager.getCache("orders") != null) {
            cacheManager.getCache("orders").clear();
            log.debug("Evicted all order caches");
        }
    }

    /**
     * Evict invoice cache entries
     */
    @CacheEvict(value = "invoices", key = "#invoiceId")
    public void evictInvoiceCache(UUID invoiceId) {
        log.debug("Evicted invoice cache for ID: {}", invoiceId);
    }

    /**
     * Evict all invoice list cache entries
     */
    public void evictAllInvoiceCaches() {
        if (cacheManager.getCache("invoices") != null) {
            cacheManager.getCache("invoices").clear();
            log.debug("Evicted all invoice caches");
        }
    }

    /**
     * Evict all caches across all cache managers
     */
    public void evictAllCaches() {
        if (cacheManager.getCacheNames() != null) {
            cacheManager.getCacheNames().forEach(cacheName -> {
                if (cacheManager.getCache(cacheName) != null) {
                    cacheManager.getCache(cacheName).clear();
                    log.info("Evicted all entries from cache: {}", cacheName);
                }
            });
        }
    }

    /**
     * Evict caches by pattern (advanced use case)
     */
    public void evictCachesByPattern(String pattern) {
        if (cacheManager.getCacheNames() != null) {
            cacheManager.getCacheNames().forEach(cacheName -> {
                if (cacheName.contains(pattern) && cacheManager.getCache(cacheName) != null) {
                    cacheManager.getCache(cacheName).clear();
                    log.info("Evicted cache matching pattern '{}': {}", pattern, cacheName);
                }
            });
        }
    }
}
