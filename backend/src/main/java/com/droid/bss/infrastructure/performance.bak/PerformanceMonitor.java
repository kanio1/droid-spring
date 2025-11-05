package com.droid.bss.infrastructure.performance;

import com.droid.bss.infrastructure.metrics.BusinessMetrics;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Performance monitoring service for tracking system performance
 */
@Component
public class PerformanceMonitor {

    private final BusinessMetrics businessMetrics;
    private final MeterRegistry meterRegistry;

    // Performance metrics
    private final Counter slowQueriesCounter;
    private final Counter timeoutErrorsCounter;
    private final Timer databaseQueryTimer;
    private final Timer externalServiceTimer;
    private final AtomicLong activeConnections = new AtomicLong(0);
    private final AtomicLong peakMemoryUsage = new AtomicLong(0);
    private final ConcurrentHashMap<String, AtomicInteger> endpointRequestCounts = new ConcurrentHashMap<>();

    // Gauge references
    private final Gauge activeConnectionsGauge;
    private final Gauge memoryUsageGauge;
    private final Gauge threadPoolSizeGauge;

    public PerformanceMonitor(BusinessMetrics businessMetrics, MeterRegistry meterRegistry) {
        this.businessMetrics = businessMetrics;
        this.meterRegistry = meterRegistry;

        // Create metrics
        this.slowQueriesCounter = Counter.builder("bss.performance.slow_queries.total")
                .description("Total number of slow database queries")
                .register(meterRegistry);
        this.timeoutErrorsCounter = Counter.builder("bss.performance.timeouts.total")
                .description("Total number of timeout errors")
                .register(meterRegistry);
        this.databaseQueryTimer = Timer.builder("bss.performance.database.query.duration")
                .description("Database query execution time")
                .register(meterRegistry);
        this.externalServiceTimer = Timer.builder("bss.performance.external_service.duration")
                .description("External service call duration")
                .register(meterRegistry);

        // Create gauges
        this.activeConnectionsGauge = meterRegistry.gauge("bss.performance.connections.active",
                activeConnections, AtomicLong::get);
        this.memoryUsageGauge = meterRegistry.gauge("bss.performance.memory.usage.bytes",
                peakMemoryUsage, AtomicLong::get);
        this.threadPoolSizeGauge = meterRegistry.gauge("bss.performance.threadpool.size",
                (AtomicInteger) new AtomicInteger(Thread.getAllStackTraces().keySet().size()),
                AtomicInteger::get);
    }

    /**
     * Record a database query execution
     */
    public void recordDatabaseQuery(String queryName, long durationMs) {
        Timer.Sample sample = Timer.start(meterRegistry);
        sample.stop(databaseQueryTimer);

        if (durationMs > 1000) { // Slow query threshold: 1 second
            slowQueriesCounter.increment();
        }

        businessMetrics.recordInvoiceProcessing(sample);
    }

    /**
     * Record external service call
     */
    public void recordExternalServiceCall(String serviceName, long durationMs) {
        Timer.Sample sample = Timer.start(meterRegistry);
        sample.stop(externalServiceTimer);

        if (durationMs > 5000) { // Timeout threshold: 5 seconds
            timeoutErrorsCounter.increment();
        }

        businessMetrics.recordServiceActivation(sample);
    }

    /**
     * Record endpoint request
     */
    public void recordEndpointRequest(String endpoint) {
        endpointRequestCounts.computeIfAbsent(endpoint, k -> new AtomicInteger(0)).incrementAndGet();
    }

    /**
     * Update active connection count
     */
    public void setActiveConnections(long count) {
        activeConnections.set(count);
    }

    /**
     * Update peak memory usage
     */
    public void setPeakMemoryUsage(long bytes) {
        peakMemoryUsage.set(bytes);
    }

    /**
     * Get memory usage in MB
     */
    public long getMemoryUsageMB() {
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        return usedMemory / (1024 * 1024);
    }

    /**
     * Get CPU usage percentage
     */
    public double getCpuUsage() {
        // This is a simplified CPU usage calculation
        // In production, you would use a proper monitoring library
        return Math.min(100.0, (double) Thread.getAllStackTraces().keySet().size() / 1000 * 100);
    }

    /**
     * Check if system is under high load
     */
    public boolean isUnderHighLoad() {
        long memoryMB = getMemoryUsageMB();
        double cpuUsage = getCpuUsage();
        long activeConnections = this.activeConnections.get();

        return memoryMB > 1024 || // 1GB memory
               cpuUsage > 80 ||    // 80% CPU
               activeConnections > 100; // 100 active connections
    }

    /**
     * Get performance report
     */
    public PerformanceReport getPerformanceReport() {
        return new PerformanceReport(
                getMemoryUsageMB(),
                getCpuUsage(),
                activeConnections.get(),
                endpointRequestCounts.size(),
                databaseQueryTimer.count(),
                externalServiceTimer.count(),
                slowQueriesCounter.count(),
                timeoutErrorsCounter.count()
        );
    }

    /**
     * Performance report data class
     */
    public static class PerformanceReport {
        private final long memoryUsageMB;
        private final double cpuUsage;
        private final long activeConnections;
        private final int uniqueEndpoints;
        private final long totalDatabaseQueries;
        private final long totalExternalCalls;
        private final long slowQueries;
        private final long timeouts;

        public PerformanceReport(long memoryUsageMB, double cpuUsage, long activeConnections,
                                int uniqueEndpoints, long totalDatabaseQueries, long totalExternalCalls,
                                long slowQueries, long timeouts) {
            this.memoryUsageMB = memoryUsageMB;
            this.cpuUsage = cpuUsage;
            this.activeConnections = activeConnections;
            this.uniqueEndpoints = uniqueEndpoints;
            this.totalDatabaseQueries = totalDatabaseQueries;
            this.totalExternalCalls = totalExternalCalls;
            this.slowQueries = slowQueries;
            this.timeouts = timeouts;
        }

        // Getters
        public long getMemoryUsageMB() { return memoryUsageMB; }
        public double getCpuUsage() { return cpuUsage; }
        public long getActiveConnections() { return activeConnections; }
        public int getUniqueEndpoints() { return uniqueEndpoints; }
        public long getTotalDatabaseQueries() { return totalDatabaseQueries; }
        public long getTotalExternalCalls() { return totalExternalCalls; }
        public long getSlowQueries() { return slowQueries; }
        public long getTimeouts() { return timeouts; }
    }
}
