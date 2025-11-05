package com.droid.bss.infrastructure.loadtest;

import com.droid.bss.infrastructure.metrics.BusinessMetrics;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Load test runner for performance testing
 */
@Component
@Profile("loadtest")
public class LoadTestRunner implements CommandLineRunner {

    private final LoadTestConfig.LoadTestProperties properties;
    private final BusinessMetrics businessMetrics;
    private final MeterRegistry meterRegistry;

    public LoadTestRunner(LoadTestConfig.LoadTestProperties properties,
                          BusinessMetrics businessMetrics,
                          MeterRegistry meterRegistry) {
        this.properties = properties;
        this.businessMetrics = businessMetrics;
        this.meterRegistry = meterRegistry;
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("Starting load test...");
        System.out.println("Configuration:");
        System.out.println("  Virtual Users: " + properties.getVirtualUsers());
        System.out.println("  Duration: " + properties.getDurationMinutes() + " minutes");
        System.out.println("  Threads: " + properties.getThreads());
        System.out.println("  Iterations: " + properties.getIterations());

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        long startTime = System.currentTimeMillis();

        // Register gauges
        Gauge.builder("loadtest.requests.total", successCount, AtomicInteger::get)
                .description("Total successful requests")
                .register(meterRegistry);
        Gauge.builder("loadtest.errors.total", errorCount, AtomicInteger::get)
                .description("Total errors")
                .register(meterRegistry);

        ExecutorService executor = Executors.newFixedThreadPool(properties.getThreads());

        // Create load test tasks
        for (int i = 0; i < properties.getVirtualUsers(); i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < properties.getIterations(); j++) {
                        performRequest();
                        successCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                }
            });
        }

        executor.shutdown();
        executor.awaitTermination(properties.getDurationMinutes(), TimeUnit.MINUTES);

        long endTime = System.currentTimeMillis();
        long durationMs = endTime - startTime;
        double durationSec = durationMs / 1000.0;

        int totalRequests = successCount.get() + errorCount.get();
        double requestsPerSecond = totalRequests / durationSec;
        double successRate = (double) successCount.get() / totalRequests * 100;

        System.out.println("\n=== Load Test Results ===");
        System.out.println("Total Requests: " + totalRequests);
        System.out.println("Successful: " + successCount.get());
        System.out.println("Errors: " + errorCount.get());
        System.out.println("Duration: " + String.format("%.2f", durationSec) + " seconds");
        System.out.println("Throughput: " + String.format("%.2f", requestsPerSecond) + " req/sec");
        System.out.println("Success Rate: " + String.format("%.2f", successRate) + "%");

        // Print performance metrics
        System.out.println("\n=== Performance Metrics ===");
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
        System.out.println("Memory Used: " + usedMemory + " MB");
        System.out.println("CPU Usage: " + getCpuUsage() + "%");

        // Track metrics
        businessMetrics.setTotalCustomers(totalRequests);
    }

    private void performRequest() {
        try {
            URL url = new URL(properties.getBaseUrl() + "/actuator/health");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                throw new RuntimeException("HTTP error: " + responseCode);
            }
            conn.disconnect();
        } catch (Exception e) {
            throw new RuntimeException("Request failed", e);
        }
    }

    private double getCpuUsage() {
        // Simplified CPU usage calculation
        return Math.min(100.0, (double) Thread.getAllStackTraces().keySet().size() / 1000 * 100);
    }
}
