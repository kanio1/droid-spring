package com.droid.bss.infrastructure.cache;

import com.droid.bss.domain.customer.CustomerRepository;
import com.droid.bss.domain.product.ProductRepository;
import com.droid.bss.domain.address.AddressRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Cache Warmer
 *
 * Preloads frequently accessed data into the cache on application startup
 * Improves response times by avoiding cold starts
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CacheWarmer implements CommandLineRunner {

    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final AddressRepository addressRepository;
    private final CacheManager cacheManager;

    private static final String CUSTOMER_CACHE = "customers";
    private static final String PRODUCT_CACHE = "products";
    private static final String ADDRESS_CACHE = "addresses";
    private static final String STATISTICS_CACHE = "statistics";

    @Override
    public void run(String... args) {
        log.info("Starting cache warming...");

        long startTime = System.currentTimeMillis();

        try {
            // Warm up caches in parallel for better performance
            ExecutorService executor = Executors.newFixedThreadPool(4);

            List<CompletableFuture<Void>> warmingTasks = List.of(
                CompletableFuture.runAsync(this::warmCustomerCache, executor),
                CompletableFuture.runAsync(this::warmProductCache, executor),
                CompletableFuture.runAsync(this::warmAddressCache, executor),
                CompletableFuture.runAsync(this::warmStatisticsCache, executor)
            );

            // Wait for all tasks to complete (max 2 minutes)
            CompletableFuture<Void> allOf = CompletableFuture.allOf(
                warmingTasks.toArray(new CompletableFuture[0])
            );

            allOf.get(2, TimeUnit.MINUTES);

            executor.shutdown();

            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            log.info("Cache warming completed in {} ms", duration);

        } catch (Exception e) {
            log.error("Error during cache warming", e);
        }
    }

    /**
     * Warm customer cache with frequently accessed customers
     */
    private void warmCustomerCache() {
        log.info("Warming customer cache...");

        try {
            Cache customerCache = cacheManager.getCache(CUSTOMER_CACHE);
            if (customerCache == null) {
                log.warn("Customer cache not found, skipping");
                return;
            }

            // Get all customers (limit to first 100 for cache warming)
            customerRepository.findAll()
                .stream()
                .limit(100)
                .forEach(customer -> {
                    try {
                        String cacheKey = "customer:" + customer.getId();
                        customerCache.put(cacheKey, customer);
                    } catch (Exception e) {
                        log.debug("Failed to cache customer: {}", customer.getId(), e);
                    }
                });

            log.info("Customer cache warming completed");

        } catch (Exception e) {
            log.error("Failed to warm customer cache", e);
        }
    }

    /**
     * Warm product cache with active products
     */
    private void warmProductCache() {
        log.info("Warming product cache...");

        try {
            Cache productCache = cacheManager.getCache(PRODUCT_CACHE);
            if (productCache == null) {
                log.warn("Product cache not found, skipping");
                return;
            }

            // Get all active products
            productRepository.findAll()
                .stream()
                .forEach(product -> {
                    try {
                        String cacheKey = "product:" + product.getId();
                        productCache.put(cacheKey, product);

                        // Also cache by SKU for quick lookup
                        if (product.getSku() != null) {
                            String skuCacheKey = "product:sku:" + product.getSku();
                            productCache.put(skuCacheKey, product);
                        }
                    } catch (Exception e) {
                        log.debug("Failed to cache product: {}", product.getId(), e);
                    }
                });

            log.info("Product cache warming completed");

        } catch (Exception e) {
            log.error("Failed to warm product cache", e);
        }
    }

    /**
     * Warm address cache with primary addresses
     */
    private void warmAddressCache() {
        log.info("Warming address cache...");

        try {
            Cache addressCache = cacheManager.getCache(ADDRESS_CACHE);
            if (addressCache == null) {
                log.warn("Address cache not found, skipping");
                return;
            }

            // Get all addresses (limit to first 1000 to avoid excessive warming)
            addressRepository.findAll()
                .stream()
                .limit(1000)
                .forEach(address -> {
                    try {
                        String cacheKey = "address:" + address.getId();
                        addressCache.put(cacheKey, address);
                    } catch (Exception e) {
                        log.debug("Failed to cache address: {}", address.getId(), e);
                    }
                });

            log.info("Address cache warming completed");

        } catch (Exception e) {
            log.error("Failed to warm address cache", e);
        }
    }

    /**
     * Warm statistics cache with precomputed statistics
     */
    private void warmStatisticsCache() {
        log.info("Warming statistics cache...");

        try {
            Cache statisticsCache = cacheManager.getCache(STATISTICS_CACHE);
            if (statisticsCache == null) {
                log.warn("Statistics cache not found, skipping");
                return;
            }

            // Cache basic statistics
            statisticsCache.put("totalCustomers", customerRepository.count());
            statisticsCache.put("totalProducts", productRepository.count());
            statisticsCache.put("totalAddresses", addressRepository.count());

            // Cache recent activity
            customerRepository.findAll()
                .stream()
                .limit(10)
                .forEach(customer -> {
                    try {
                        String cacheKey = "recentCustomer:" + customer.getId();
                        statisticsCache.put(cacheKey, customer.getCreatedAt());
                    } catch (Exception e) {
                        log.debug("Failed to cache recent customer timestamp", e);
                    }
                });

            log.info("Statistics cache warming completed");

        } catch (Exception e) {
            log.error("Failed to warm statistics cache", e);
        }
    }

    /**
     * Warm specific entities by ID
     */
    public void warmEntity(String cacheName, UUID entityId, Object entity) {
        try {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                String cacheKey = cacheName + ":" + entityId;
                cache.put(cacheKey, entity);
                log.debug("Warmed cache: {} with key: {}", cacheName, cacheKey);
            }
        } catch (Exception e) {
            log.error("Failed to warm entity in cache: {}", cacheName, e);
        }
    }

    /**
     * Invalidate specific cache
     */
    public void invalidateCache(String cacheName) {
        try {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
                log.info("Invalidated cache: {}", cacheName);
            }
        } catch (Exception e) {
            log.error("Failed to invalidate cache: {}", cacheName, e);
        }
    }

    /**
     * Get cache statistics
     */
    public void printCacheStatistics() {
        log.info("=== Cache Statistics ===");

        cacheManager.getCacheNames().forEach(cacheName -> {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                try {
                    Object nativeCache = cache.getNativeCache();
                    if (nativeCache instanceof com.github.benmanes.caffeine.cache.Cache) {
                        com.github.benmanes.caffeine.cache.Cache<?, ?> caffeineCache =
                            (com.github.benmanes.caffeine.cache.Cache<?, ?>) nativeCache;

                        com.github.benmanes.caffeine.cache.CacheStats stats = caffeineCache.stats();

                        log.info("Cache: {}", cacheName);
                        log.info("  Hit Rate: {}%", String.format("%.2f", stats.hitRate() * 100));
                        log.info("  Miss Rate: {}%", String.format("%.2f", stats.missRate() * 100));
                        log.info("  Size: {}", stats.requestCount());
                        log.info("  Entries: {}", caffeineCache.estimatedSize());
                    }
                } catch (Exception e) {
                    log.debug("Could not get statistics for cache: {}", cacheName, e);
                }
            }
        });
    }
}
