package com.droid.bss.infrastructure.cache.advanced;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Cache Warming Service
 * Pre-populates cache with frequently accessed data
 */
@Slf4j
@Service
public class CacheWarmingService {

    private final MultiLayerCacheManager cacheManager;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final Map<String, CacheWarmingTask> warmingTasks = new ConcurrentHashMap<>();
    private volatile boolean enabled = true;
    private Duration warmingInterval = Duration.ofMinutes(10);

    public CacheWarmingService(MultiLayerCacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    /**
     * Add a cache warming task
     */
    public void addWarmingTask(String name, CacheWarmingTask task) {
        warmingTasks.put(name, task);
        log.info("Added cache warming task: {}", name);
    }

    /**
     * Start automatic cache warming
     */
    public void startWarming() {
        if (!enabled) {
            log.info("Cache warming is disabled");
            return;
        }

        log.info("Starting cache warming service");

        scheduler.scheduleAtFixedRate(this::performWarming, 1, warmingInterval.toMinutes(), TimeUnit.MINUTES);

        // Perform initial warming
        performInitialWarming();
    }

    /**
     * Stop cache warming
     */
    public void stopWarming() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("Cache warming service stopped");
    }

    /**
     * Manually trigger warming
     */
    public void warmAll() {
        log.info("Manually triggering cache warming");
        performWarming();
    }

    /**
     * Perform initial warming
     */
    private void performInitialWarming() {
        log.info("Performing initial cache warming");

        // Warm hot keys
        warmHotKeys();

        // Warm queries
        warmCommonQueries();

        // Warm aggregates
        warmCommonAggregates();

        log.info("Initial cache warming completed");
    }

    /**
     * Perform periodic warming
     */
    private void performWarming() {
        try {
            log.debug("Performing periodic cache warming");

            performInitialWarming();

            // Execute custom warming tasks
            for (Map.Entry<String, CacheWarmingTask> entry : warmingTasks.entrySet()) {
                try {
                    log.trace("Executing warming task: {}", entry.getKey());
                    entry.getValue().warm(cacheManager);
                } catch (Exception e) {
                    log.warn("Warming task failed: {}", entry.getKey(), e);
                }
            }

            log.debug("Cache warming completed successfully");

        } catch (Exception e) {
            log.error("Cache warming failed", e);
        }
    }

    /**
     * Warm hot keys
     */
    private void warmHotKeys() {
        // Simulate warming frequently accessed data
        List<String> hotEntities = Arrays.asList(
            "customers:all",
            "products:featured",
            "orders:recent",
            "invoices:pending",
            "payments:processed"
        );

        for (String key : hotEntities) {
            // Simulate loading data
            String value = "warmed-data-" + key;
            cacheManager.put(key, value, Duration.ofMinutes(30));
        }

        log.info("Warmed {} hot keys", hotEntities.size());
    }

    /**
     * Warm common queries
     */
    private void warmCommonQueries() {
        Map<String, Object> commonQueries = new HashMap<>();
        commonQueries.put("customers:status:ACTIVE", getActiveCustomers());
        commonQueries.put("products:category:FEATURED", getFeaturedProducts());
        commonQueries.put("orders:status:PENDING", getPendingOrders());

        for (Map.Entry<String, Object> entry : commonQueries.entrySet()) {
            cacheManager.put(entry.getKey(), entry.getValue(), Duration.ofMinutes(30));
        }

        log.info("Warmed {} common queries", commonQueries.size());
    }

    /**
     * Warm common aggregates
     */
    private void warmCommonAggregates() {
        Map<String, Object> aggregates = new HashMap<>();
        aggregates.put("revenue:monthly", getMonthlyRevenue());
        aggregates.put("orders:daily-count", getDailyOrderCount());
        aggregates.put("customers:active-count", getActiveCustomerCount());

        for (Map.Entry<String, Object> entry : aggregates.entrySet()) {
            cacheManager.put(entry.getKey(), entry.getValue(), Duration.ofMinutes(30));
        }

        log.info("Warmed {} common aggregates", aggregates.size());
    }

    // Simulated data generators
    private List<Map<String, Object>> getActiveCustomers() {
        return Arrays.asList(
            Map.of("id", "1", "name", "Customer 1", "status", "ACTIVE"),
            Map.of("id", "2", "name", "Customer 2", "status", "ACTIVE")
        );
    }

    private List<Map<String, Object>> getFeaturedProducts() {
        return Arrays.asList(
            Map.of("id", "1", "name", "Product 1", "featured", true),
            Map.of("id", "2", "name", "Product 2", "featured", true)
        );
    }

    private List<Map<String, Object>> getPendingOrders() {
        return Arrays.asList(
            Map.of("id", "1", "status", "PENDING", "total", 99.99),
            Map.of("id", "2", "status", "PENDING", "total", 199.99)
        );
    }

    private Map<String, Object> getMonthlyRevenue() {
        return Map.of(
            "month", "2025-01",
            "total", 50000.00,
            "currency", "USD"
        );
    }

    private Map<String, Object> getDailyOrderCount() {
        return Map.of(
            "date", "2025-01-15",
            "count", 42
        );
    }

    private Map<String, Object> getActiveCustomerCount() {
        return Map.of(
            "status", "ACTIVE",
            "count", 128
        );
    }

    /**
     * Set whether warming is enabled
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        log.info("Cache warming {}", enabled ? "enabled" : "disabled");
    }

    /**
     * Set warming interval
     */
    public void setWarmingInterval(Duration interval) {
        this.warmingInterval = interval;
    }

    /**
     * Get warming statistics
     */
    public Map<String, Object> getWarmingStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("enabled", enabled);
        stats.put("intervalMinutes", warmingInterval.toMinutes());
        stats.put("registeredTasks", warmingTasks.size());
        return stats;
    }

    /**
     * Cache warming task interface
     */
    public interface CacheWarmingTask {
        void warm(MultiLayerCacheManager cacheManager);
    }
}
