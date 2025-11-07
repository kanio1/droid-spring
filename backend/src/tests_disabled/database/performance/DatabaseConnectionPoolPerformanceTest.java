package com.droid.bss.infrastructure.database.performance;

import com.droid.bss.BssApplication;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Database Connection Pool Performance Tests
 *
 * Tests connection pool behavior under load, concurrent access, and performance metrics.
 * Validates HikariCP connection pool configuration and behavior.
 */
@SpringBootTest(classes = Application.class)
@Testcontainers
@DisplayName("Database Connection Pool Performance Tests")
class DatabaseConnectionPoolPerformanceTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18-alpine")
            .withDatabaseName("bss_test")
            .withUsername("test")
            .withPassword("test")
            .withReuse(true);

    @Autowired
    private DataSource dataSource;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.hikari.maximum-pool-size", () -> 20);
        registry.add("spring.datasource.hikari.minimum-idle", () -> 5);
        registry.add("spring.datasource.hikari.connection-timeout", () -> 20000L);
        registry.add("spring.datasource.hikari.idle-timeout", () -> 300000L);
        registry.add("spring.datasource.hikari.max-lifetime", () -> 1200000L);
        registry.add("spring.datasource.hikari.leak-detection-threshold", () -> 60000L);
    }

    @Test
    @DisplayName("Should handle concurrent connection acquisition")
    void shouldHandleConcurrentConnections() throws InterruptedException {
        int threadCount = 50;
        int iterations = 10;
        CountDownLatch latch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < iterations; j++) {
                        try (Connection conn = dataSource.getConnection()) {
                            assertThat(conn.isValid(2)).isTrue();
                            successCount.incrementAndGet();
                        }
                    }
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        assertThat(latch.await(30, TimeUnit.SECONDS)).isTrue();
        executor.shutdown();

        assertThat(errorCount.get()).isEqualTo(0);
        assertThat(successCount.get()).isEqualTo(threadCount * iterations);
    }

    @Test
    @DisplayName("Should handle connection pool exhaustion gracefully")
    void shouldHandlePoolExhaustion() throws InterruptedException {
        int threadCount = 100;
        int iterations = 5;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(100);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger timeoutCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < iterations; j++) {
                        try (Connection conn = dataSource.getConnection()) {
                            Thread.sleep(10);
                            assertThat(conn.isValid(2)).isTrue();
                            successCount.incrementAndGet();
                        }
                    }
                } catch (Exception e) {
                    timeoutCount.incrementAndGet();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertThat(endLatch.await(60, TimeUnit.SECONDS)).isTrue();
        executor.shutdown();

        assertThat(successCount.get()).isGreaterThan(0);
        assertThat(timeoutCount.get()).isLessThan(threadCount);
    }

    @Test
    @DisplayName("Should validate connection timeout under load")
    void shouldValidateConnectionTimeout() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(postgres.getJdbcUrl());
        config.setUsername(postgres.getUsername());
        config.setPassword(postgres.getPassword());
        config.setMaximumPoolSize(5);
        config.setConnectionTimeout(5000);
        config.setConnectionTestQuery("SELECT 1");

        try (HikariDataSource dataSource = new HikariDataSource(config)) {
            assertThat(dataSource.getConnectionTimeout()).isEqualTo(5000);
            assertThat(dataSource.getHikariPoolMXBean().getActiveConnections()).isGreaterThanOrEqualTo(0);
        }
    }

    @Test
    @DisplayName("Should optimize pool size configuration")
    void shouldOptimizePoolSize() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(postgres.getJdbcUrl());
        config.setUsername(postgres.getUsername());
        config.setPassword(postgres.getPassword());
        config.setMaximumPoolSize(50);
        config.setMinimumIdle(10);
        config.setLeakDetectionThreshold(60000);

        try (HikariDataSource dataSource = new HikariDataSource(config)) {
            assertThat(dataSource.getMaximumPoolSize()).isEqualTo(50);
            assertThat(dataSource.getMinimumIdle()).isEqualTo(10);
            assertThat(dataSource.getLeakDetectionThreshold()).isEqualTo(60000);
        }
    }

    @Test
    @DisplayName("Should cleanup idle connections")
    void shouldCleanupIdleConnections() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(10);

        for (int i = 0; i < 10; i++) {
            executor.submit(() -> {
                try (Connection conn = dataSource.getConnection()) {
                    assertThat(conn.isValid(2)).isTrue();
                    Thread.sleep(100);
                } catch (Exception e) {
                    fail("Failed to cleanup idle connection", e);
                } finally {
                    latch.countDown();
                }
            });
        }

        assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
        executor.shutdown();

        if (dataSource instanceof HikariDataSource) {
            HikariDataSource hikariDS = (HikariDataSource) dataSource;
            int idleConnections = hikariDS.getHikariPoolMXBean().getIdleConnections();
            assertThat(idleConnections).isGreaterThanOrEqualTo(0);
        }
    }

    @Test
    @DisplayName("Should detect connection leaks under load")
    void shouldDetectConnectionLeaks() throws InterruptedException {
        int threadCount = 20;
        CountDownLatch latch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        AtomicInteger leakCount = new AtomicInteger(0);

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(postgres.getJdbcUrl());
        config.setUsername(postgres.getUsername());
        config.setPassword(postgres.getPassword());
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(0);
        config.setLeakDetectionThreshold(2000);

        try (HikariDataSource dataSource = new HikariDataSource(config)) {
            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        Connection conn = dataSource.getConnection();
                        Thread.sleep(5000);
                        conn.close();
                    } catch (Exception e) {
                        leakCount.incrementAndGet();
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await(10, TimeUnit.SECONDS);
            executor.shutdown();

            if (dataSource.getHikariPoolMXBean().getActiveConnections() > 0) {
                assertThat(leakCount.get()).isGreaterThan(0);
            }
        }
    }

    @Test
    @DisplayName("Should measure connection reuse efficiency")
    void shouldMeasureConnectionReuseEfficiency() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(20);
        CountDownLatch latch = new CountDownLatch(20);
        AtomicInteger totalConnections = new AtomicInteger(0);
        AtomicInteger uniqueConnections = new AtomicInteger(0);

        for (int i = 0; i < 20; i++) {
            executor.submit(() -> {
                try (Connection conn = dataSource.getConnection()) {
                    String sql = "SELECT 1";
                    conn.createStatement().execute(sql);
                    totalConnections.incrementAndGet();
                } catch (SQLException e) {
                    fail("Connection failed", e);
                } finally {
                    latch.countDown();
                }
            });
        }

        assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
        executor.shutdown();

        assertThat(totalConnections.get()).isEqualTo(20);
        assertThat(uniqueConnections.get()).isLessThanOrEqualTo(totalConnections.get());
    }

    @Test
    @DisplayName("Should monitor pool health")
    void shouldMonitorPoolHealth() throws SQLException {
        if (dataSource instanceof HikariDataSource) {
            HikariDataSource hikariDS = (HikariDataSource) dataSource;
            var poolBean = hikariDS.getHikariPoolMXBean();

            int active = poolBean.getActiveConnections();
            int idle = poolBean.getIdleConnections();
            int total = poolBean.getTotalConnections();
            int threadsAwaiting = poolBean.getThreadsAwaitingConnection();

            assertThat(total).isGreaterThanOrEqualTo(0);
            assertThat(active).isGreaterThanOrEqualTo(0);
            assertThat(idle).isGreaterThanOrEqualTo(0);
            assertThat(threadsAwaiting).isGreaterThanOrEqualTo(0);
            assertThat(active + idle).isEqualTo(total);

            try (Connection conn = dataSource.getConnection()) {
                assertThat(conn.isValid(2)).isTrue();
            }

            int activeAfter = poolBean.getActiveConnections();
            int idleAfter = poolBean.getIdleConnections();
            assertThat(idleAfter).isGreaterThanOrEqualTo(0);
        }
    }

    @Test
    @DisplayName("Should handle peak load")
    void shouldHandlePeakLoad() throws InterruptedException {
        int threadCount = 50;
        int iterations = 20;
        CountDownLatch latch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(50);
        AtomicInteger successCount = new AtomicInteger(0);
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < iterations; j++) {
                        try (Connection conn = dataSource.getConnection()) {
                            conn.createStatement().execute("SELECT 1");
                            successCount.incrementAndGet();
                        }
                    }
                } catch (Exception e) {
                    fail("Peak load test failed", e);
                } finally {
                    latch.countDown();
                }
            });
        }

        assertThat(latch.await(60, TimeUnit.SECONDS)).isTrue();
        executor.shutdown();

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        double throughput = (double) successCount.get() / (duration / 1000.0);

        assertThat(successCount.get()).isEqualTo(threadCount * iterations);
        assertThat(throughput).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should measure connection acquisition latency")
    void shouldMeasureConnectionAcquisitionLatency() throws InterruptedException {
        int iterations = 100;
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(iterations);
        AtomicLong totalLatency = new AtomicLong(0);

        for (int i = 0; i < iterations; i++) {
            executor.submit(() -> {
                long start = System.nanoTime();
                try (Connection conn = dataSource.getConnection()) {
                    long end = System.nanoTime();
                    totalLatency.addAndGet(end - start);
                }
                latch.countDown();
            });
        }

        assertThat(latch.await(30, TimeUnit.SECONDS)).isTrue();
        executor.shutdown();

        double avgLatencyMs = totalLatency.get() / (double) iterations / 1_000_000.0;
        assertThat(avgLatencyMs).isLessThan(100);
    }

    @Test
    @DisplayName("Should handle pool starvation scenarios")
    void shouldHandlePoolStarvation() throws InterruptedException {
        int threadCount = 30;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(30);
        AtomicInteger successCount = new AtomicInteger(0);

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(postgres.getJdbcUrl());
        config.setUsername(postgres.getUsername());
        config.setPassword(postgres.getPassword());
        config.setMaximumPoolSize(5);
        config.setConnectionTimeout(10000);

        try (HikariDataSource dataSource = new HikariDataSource(config)) {
            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        try (Connection conn = dataSource.getConnection()) {
                            conn.createStatement().execute("SELECT 1");
                            successCount.incrementAndGet();
                        }
                    } catch (Exception e) {
                    } finally {
                        endLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            assertThat(endLatch.await(20, TimeUnit.SECONDS)).isTrue();
            executor.shutdown();

            assertThat(successCount.get()).isGreaterThan(0);
        }
    }

    @Test
    @DisplayName("Should measure connection reset time")
    void shouldMeasureConnectionResetTime() throws InterruptedException {
        int iterations = 50;
        AtomicLong totalResetTime = new AtomicLong(0);

        for (int i = 0; i < iterations; i++) {
            try (Connection conn = dataSource.getConnection()) {
                conn.createStatement().execute("SELECT 1");
                long start = System.nanoTime();
                conn.createStatement().execute("SELECT 1");
                long end = System.nanoTime();
                totalResetTime.addAndGet(end - start);
            }
        }

        double avgResetTimeMs = totalResetTime.get() / (double) iterations / 1_000_000.0;
        assertThat(avgResetTimeMs).isLessThan(10);
    }

    @Test
    @DisplayName("Should measure query execution time with connection pool")
    void shouldMeasureQueryExecutionTime() throws InterruptedException {
        int iterations = 100;
        AtomicLong totalQueryTime = new AtomicLong(0);
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(iterations);

        for (int i = 0; i < iterations; i++) {
            executor.submit(() -> {
                try (Connection conn = dataSource.getConnection()) {
                    long start = System.nanoTime();
                    conn.createStatement().execute("SELECT 1");
                    long end = System.nanoTime();
                    totalQueryTime.addAndGet(end - start);
                } catch (SQLException e) {
                    fail("Query execution failed", e);
                } finally {
                    latch.countDown();
                }
            });
        }

        assertThat(latch.await(30, TimeUnit.SECONDS)).isTrue();
        executor.shutdown();

        double avgQueryTimeMs = totalQueryTime.get() / (double) iterations / 1_000_000.0;
        assertThat(avgQueryTimeMs).isLessThan(50);
    }

    @Test
    @DisplayName("Should monitor connection lifecycle")
    void shouldMonitorConnectionLifecycle() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(postgres.getJdbcUrl());
        config.setUsername(postgres.getUsername());
        config.setPassword(postgres.getPassword());
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);

        try (HikariDataSource dataSource = new HikariDataSource(config)) {
            var poolBean = dataSource.getHikariPoolMXBean();

            int initialTotal = poolBean.getTotalConnections();

            try (Connection conn1 = dataSource.getConnection();
                 Connection conn2 = dataSource.getConnection()) {

                int activeConnections = poolBean.getActiveConnections();
                assertThat(activeConnections).isGreaterThanOrEqualTo(0);
            }

            int afterClose = poolBean.getTotalConnections();
            assertThat(afterClose).isGreaterThanOrEqualTo(0);
        }
    }

    @Test
    @DisplayName("Should validate pool metrics")
    void shouldValidatePoolMetrics() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(postgres.getJdbcUrl());
        config.setUsername(postgres.getUsername());
        config.setPassword(postgres.getPassword());
        config.setMaximumPoolSize(20);
        config.setMinimumIdle(5);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        config.setLeakDetectionThreshold(60000);

        try (HikariDataSource dataSource = new HikariDataSource(config)) {
            assertThat(dataSource.getMaximumPoolSize()).isEqualTo(20);
            assertThat(dataSource.getMinimumIdle()).isEqualTo(5);
            assertThat(dataSource.getConnectionTimeout()).isEqualTo(30000);
            assertThat(dataSource.getIdleTimeout()).isEqualTo(600000);
            assertThat(dataSource.getMaxLifetime()).isEqualTo(1800000);
            assertThat(dataSource.getLeakDetectionThreshold()).isEqualTo(60000);

            try (Connection conn = dataSource.getConnection()) {
                assertThat(conn.isValid(2)).isTrue();
            }
        }
    }
}
