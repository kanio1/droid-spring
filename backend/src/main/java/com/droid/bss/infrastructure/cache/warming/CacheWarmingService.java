package com.droid.bss.infrastructure.cache.warming;

import com.droid.bss.application.query.customer.CustomerQueryService;
import com.droid.bss.application.query.order.OrderQueryService;
import com.droid.bss.application.query.invoice.InvoiceQueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Cache Warming Service
 *
 * Pre-loads frequently accessed data into cache during application startup
 * Improves performance by ensuring popular data is cached before user requests
 */
@Service
public class CacheWarmingService implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(CacheWarmingService.class);
    private final CustomerQueryService customerQueryService;
    private final OrderQueryService orderQueryService;
    private final InvoiceQueryService invoiceQueryService;

    private final ExecutorService warmingExecutor = Executors.newFixedThreadPool(4);

    public CacheWarmingService(
            CustomerQueryService customerQueryService,
            OrderQueryService orderQueryService,
            InvoiceQueryService invoiceQueryService
    ) {
        this.customerQueryService = customerQueryService;
        this.orderQueryService = orderQueryService;
        this.invoiceQueryService = invoiceQueryService;
    }

    @Override
    public void run(String... args) {
        log.info("Starting cache warming process...");

        try {
            warmAllCaches();
            log.info("Cache warming completed successfully");
        } catch (Exception e) {
            log.error("Error during cache warming: {}", e.getMessage(), e);
        } finally {
            warmingExecutor.shutdown();
        }
    }

    /**
     * Warm all caches
     */
    public void warmAllCaches() {
        List<CompletableFuture<Void>> warmingTasks = List.of(
                warmCustomerCache(),
                warmOrderCache(),
                warmInvoiceCache()
        );

        CompletableFuture.allOf(warmingTasks.toArray(new CompletableFuture[0]))
                .join(); // Wait for all tasks to complete
    }

    /**
     * Warm customer cache with frequently accessed data
     */
    public CompletableFuture<Void> warmCustomerCache() {
        return CompletableFuture.runAsync(() -> {
            log.info("Warming customer cache...");

            try {
                // Warm active customers
                var activeCustomers = customerQueryService.findByStatus("ACTIVE", 0, 20, "createdAt");
                log.info("Warmed {} active customers into cache", activeCustomers.content().size());

                // Warm recent customers
                var recentCustomers = customerQueryService.findAll(0, 20, "createdAt");
                log.info("Warmed {} recent customers into cache", recentCustomers.content().size());

                log.info("Customer cache warming completed");
            } catch (Exception e) {
                log.error("Error warming customer cache: {}", e.getMessage(), e);
            }
        }, warmingExecutor);
    }

    /**
     * Warm order cache with frequently accessed data
     */
    public CompletableFuture<Void> warmOrderCache() {
        return CompletableFuture.runAsync(() -> {
            log.info("Warming order cache...");

            try {
                // Warm pending orders
                // Note: In a real implementation, you'd use proper enums
                // var pendingOrders = orderQueryService.findByStatus(OrderStatus.PENDING, 0, 20);
                // log.info("Warmed {} pending orders into cache", pendingOrders.content().size());

                // Warm recent orders
                var recentOrders = orderQueryService.findAll(0, 20);
                log.info("Warmed {} recent orders into cache", recentOrders.content().size());

                log.info("Order cache warming completed");
            } catch (Exception e) {
                log.error("Error warming order cache: {}", e.getMessage(), e);
            }
        }, warmingExecutor);
    }

    /**
     * Warm invoice cache with frequently accessed data
     */
    public CompletableFuture<Void> warmInvoiceCache() {
        return CompletableFuture.runAsync(() -> {
            log.info("Warming invoice cache...");

            try {
                // Warm recent invoices
                var recentInvoices = invoiceQueryService.findAll(0, 20);
                log.info("Warmed {} recent invoices into cache", recentInvoices.content().size());

                // Warm unpaid invoices (important for collection processes)
                // var unpaidInvoices = invoiceQueryService.findUnpaid(0, 20);
                // log.info("Warmed {} unpaid invoices into cache", unpaidInvoices.content().size());

                log.info("Invoice cache warming completed");
            } catch (Exception e) {
                log.error("Error warming invoice cache: {}", e.getMessage(), e);
            }
        }, warmingExecutor);
    }

    /**
     * Warm a specific cache on demand (e.g., during low-traffic periods)
     */
    public CompletableFuture<Void> warmCacheOnDemand(CacheType cacheType) {
        log.info("Warming cache on demand: {}", cacheType);

        return CompletableFuture.runAsync(() -> {
            try {
                switch (cacheType) {
                    case CUSTOMER -> warmCustomerCache().join();
                    case ORDER -> warmOrderCache().join();
                    case INVOICE -> warmInvoiceCache().join();
                    case ALL -> warmAllCaches();
                }
                log.info("On-demand cache warming completed for: {}", cacheType);
            } catch (Exception e) {
                log.error("Error during on-demand cache warming for {}: {}", cacheType, e.getMessage(), e);
            }
        }, warmingExecutor);
    }

    /**
     * Get cache warming statistics
     */
    public CacheWarmingStatistics getStatistics() {
        return new CacheWarmingStatistics(
                "customer",
                System.currentTimeMillis(), // last warmed timestamp
                20, // example count
                CacheWarmingStatus.COMPLETED
        );
    }
}
