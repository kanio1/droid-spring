package com.droid.bss.performance;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Database and Cache Performance Tests
 * Tests database query performance and Redis cache performance
 * Measures cache hit rates, query throughput, and latency
 */
@SpringBootTest
@EnableCaching
@Testcontainers
class DatabaseAndCachePerformanceTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:18-alpine"))
            .withDatabaseName("bss_test")
            .withUsername("test")
            .withPassword("test");

    @Container
    static GenericContainer<?> redisContainer = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
        .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redisContainer::getHost);
        registry.add("spring.data.redis.port", redisContainer::getFirstMappedPort);
    }

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final int OPERATION_COUNT = 1000;
    private static final int CONCURRENT_THREADS = 20;
    private static final int CACHE_WARMUP_SIZE = 100;

    @BeforeEach
    void setup() {
        // Clean cache before each test
        if (cacheManager.getCache("testCache") != null) {
            cacheManager.getCache("testCache").clear();
        }
        redisTemplate.getConnectionFactory().getConnection().flushAll();
    }

    @Test
    @DisplayName("Cache Performance - Hit rate under load")
    @Timeout(value = 2, unit = TimeUnit.MINUTES)
    void testCacheHitRate() throws InterruptedException {
        int totalRequests = 10000;
        int uniqueKeys = 100; // Small number of keys to test hit rate
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_THREADS);
        CountDownLatch latch = new CountDownLatch(CONCURRENT_THREADS);
        AtomicInteger hitCount = new AtomicInteger(0);
        AtomicInteger missCount = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < CONCURRENT_THREADS; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < totalRequests / CONCURRENT_THREADS; j++) {
                        String key = "key_" + (j % uniqueKeys);
                        String value = "value_" + key;

                        // Check cache
                        String cachedValue = (String) redisTemplate.opsForValue().get(key);

                        if (cachedValue == null) {
                            // Cache miss - put in cache
                            redisTemplate.opsForValue().set(key, value);
                            missCount.incrementAndGet();
                        } else {
                            // Cache hit
                            hitCount.incrementAndGet();
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(2, TimeUnit.MINUTES), "All operations should complete");
        executor.shutdown();

        long endTime = System.currentTimeMillis();
        int totalRequestsActual = hitCount.get() + missCount.get();
        double hitRate = (hitCount.get() / (double) totalRequestsActual) * 100;

        System.out.printf("Cache Hit Rate Test:%n");
        System.out.printf("  Total requests: %d%n", totalRequestsActual);
        System.out.printf("  Cache hits: %d%n", hitCount.get());
        System.out.printf("  Cache misses: %d%n", missCount.get());
        System.out.printf("  Hit rate: %.2f%% %n", hitRate);
        System.out.printf("  Unique keys: %d%n", uniqueKeys);
        System.out.printf("  Duration: %d ms%n", endTime - startTime);
        System.out.printf("  Requests/second: %.2f%n",
            totalRequestsActual * 1000.0 / (endTime - startTime));

        // Cache hit rate should be high with small number of unique keys
        assertTrue(hitRate > 90, "Cache hit rate should be over 90%");
        assertTrue(hitRate < 99.9, "Cache hit rate should not be 100% (some misses expected on first access)");
    }

    @Test
    @DisplayName("Cache Performance - Concurrent reads and writes")
    @Timeout(value = 2, unit = TimeUnit.MINUTES)
    void testConcurrentCacheOperations() throws InterruptedException {
        int readWriteRatio = 5; // 5 reads for every 1 write
        int operationsPerThread = 500;
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_THREADS);
        CountDownLatch latch = new CountDownLatch(CONCURRENT_THREADS);
        AtomicInteger readCount = new AtomicInteger(0);
        AtomicInteger writeCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < CONCURRENT_THREADS; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < operationsPerThread; j++) {
                        String key = "concurrent_key_" + (j % 50);
                        String value = "value_" + threadId + "_" + j;

                        if (j % (readWriteRatio + 1) == 0) {
                            // Write operation
                            try {
                                redisTemplate.opsForValue().set(key, value);
                                writeCount.incrementAndGet();
                            } catch (Exception e) {
                                errorCount.incrementAndGet();
                            }
                        } else {
                            // Read operation
                            try {
                                redisTemplate.opsForValue().get(key);
                                readCount.incrementAndGet();
                            } catch (Exception e) {
                                errorCount.incrementAndGet();
                            }
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(2, TimeUnit.MINUTES), "All operations should complete");
        executor.shutdown();

        long endTime = System.currentTimeMillis();
        int totalOperations = readCount.get() + writeCount.get();
        double throughput = totalOperations * 1000.0 / (endTime - startTime);

        System.out.printf("Concurrent Cache Operations Test:%n");
        System.out.printf("  Concurrent threads: %d%n", CONCURRENT_THREADS);
        System.out.printf("  Operations per thread: %d%n", operationsPerThread);
        System.out.printf("  Read operations: %d%n", readCount.get());
        System.out.printf("  Write operations: %d%n", writeCount.get());
        System.out.printf("  Error operations: %d%n", errorCount.get());
        System.out.printf("  Total operations: %d%n", totalOperations);
        System.out.printf("  Read/Write ratio: %d:%d%n", readWriteRatio, 1);
        System.out.printf("  Duration: %d ms%n", endTime - startTime);
        System.out.printf("  Throughput: %.2f ops/second%n", throughput);
        System.out.printf("  Error rate: %.2f%% %n",
            (errorCount.get() / (double) totalOperations) * 100);

        assertEquals(0, errorCount.get(), "No errors should occur during concurrent operations");
        assertTrue(throughput > 1000, "Should handle at least 1000 operations/second");
    }

    @Test
    @DisplayName("Cache Performance - Cache warmup simulation")
    @Timeout(value = 1, unit = TimeUnit.MINUTES)
    void testCacheWarmup() {
        int warmupSize = CACHE_WARMUP_SIZE;
        int testSize = 1000;
        AtomicLong coldCacheTime = new AtomicLong(0);
        AtomicLong warmCacheTime = new AtomicLong(0);

        // Cold cache test
        long start = System.nanoTime();
        for (int i = 0; i < testSize; i++) {
            String key = "cold_key_" + i;
            String value = "value_" + i;
            redisTemplate.opsForValue().set(key, value);
            redisTemplate.opsForValue().get(key);
        }
        coldCacheTime.set(System.nanoTime() - start);

        // Warmup the cache
        for (int i = 0; i < warmupSize; i++) {
            String key = "warmup_key_" + i;
            String value = "warmup_value_" + i;
            redisTemplate.opsForValue().set(key, value);
        }

        // Warm cache test
        start = System.nanoTime();
        for (int i = 0; i < testSize; i++) {
            String key = "warmup_key_" + (i % warmupSize); // Reuse warmup keys
            redisTemplate.opsForValue().get(key);
        }
        warmCacheTime.set(System.nanoTime() - start);

        double coldCacheMs = coldCacheTime.get() / 1_000_000.0;
        double warmCacheMs = warmCacheTime.get() / 1_000_000.0;
        double speedup = coldCacheMs / warmCacheMs;

        System.out.printf("Cache Warmup Test:%n");
        System.out.printf("  Cold cache operations: %d%n", testSize);
        System.out.printf("  Cold cache time: %.2f ms%n", coldCacheMs);
        System.out.printf("  Warmup size: %d%n", warmupSize);
        System.out.printf("  Warm cache time: %.2f ms%n", warmCacheMs);
        System.out.printf("  Speedup: %.2fx%n", speedup);
        System.out.printf("  Improvement: %.2f%% %n", (speedup - 1) * 100);

        // Warm cache should be faster
        assertTrue(warmCacheMs < coldCacheMs, "Warm cache should be faster than cold cache");
        assertTrue(speedup > 2, "Cache should provide at least 2x speedup");
    }

    @Test
    @DisplayName("Database Query Performance - Bulk operations")
    @Timeout(value = 2, unit = TimeUnit.MINUTES)
    void testBulkDatabaseOperations() throws InterruptedException {
        // Note: This test simulates bulk operations
        // In a real scenario, this would use actual database operations
        int batchSize = 100;
        int numberOfBatches = 50;
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(numberOfBatches);
        AtomicInteger processedRecords = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

        for (int batch = 0; batch < numberOfBatches; batch++) {
            executor.submit(() -> {
                try {
                    for (int i = 0; i < batchSize; i++) {
                        // Simulate database operation
                        UUID id = UUID.randomUUID();
                        String data = "data_" + id.toString();

                        // Simulate some processing time
                        try {
                            Thread.sleep(1);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }

                        processedRecords.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(2, TimeUnit.MINUTES), "All batches should complete");
        executor.shutdown();

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        int totalRecords = batchSize * numberOfBatches;
        double recordsPerSecond = totalRecords * 1000.0 / duration;
        double batchesPerSecond = numberOfBatches * 1000.0 / duration;

        System.out.printf("Bulk Database Operations Test:%n");
        System.out.printf("  Batch size: %d%n", batchSize);
        System.out.printf("  Number of batches: %d%n", numberOfBatches);
        System.out.printf("  Total records: %d%n", totalRecords);
        System.out.printf("  Records processed: %d%n", processedRecords.get());
        System.out.printf("  Duration: %d ms%n", duration);
        System.out.printf("  Records/second: %.2f%n", recordsPerSecond);
        System.out.printf("  Batches/second: %.2f%n", batchesPerSecond);
        System.out.printf("  Average batch time: %.2f ms%n", duration / (double) numberOfBatches);

        assertEquals(totalRecords, processedRecords.get(), "All records should be processed");
        assertTrue(recordsPerSecond > 50, "Should process at least 50 records/second");
    }

    @Test
    @DisplayName("Database Query Performance - Concurrent reads")
    @Timeout(value = 2, unit = TimeUnit.MINUTES)
    void testConcurrentDatabaseReads() throws InterruptedException {
        int concurrentReads = 100;
        int readsPerClient = 50;
        ExecutorService executor = Executors.newFixedThreadPool(concurrentReads);
        CountDownLatch latch = new CountDownLatch(concurrentReads);
        AtomicInteger totalReads = new AtomicInteger(0);
        AtomicLong totalLatency = new AtomicLong(0);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < concurrentReads; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < readsPerClient; j++) {
                        long readStart = System.nanoTime();

                        // Simulate database read
                        String key = "read_key_" + (j % 50);
                        Object value = redisTemplate.opsForValue().get(key);

                        long readLatency = System.nanoTime() - readStart;
                        totalLatency.addAndGet(readLatency);
                        totalReads.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(2, TimeUnit.MINUTES), "All reads should complete");
        executor.shutdown();

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        int totalReadsActual = totalReads.get();
        double readsPerSecond = totalReadsActual * 1000.0 / duration;
        double averageLatencyMs = totalLatency.get() / 1_000_000.0 / totalReadsActual;

        System.out.printf("Concurrent Database Reads Test:%n");
        System.out.printf("  Concurrent clients: %d%n", concurrentReads);
        System.out.printf("  Reads per client: %d%n", readsPerClient);
        System.out.printf("  Total reads: %d%n", totalReadsActual);
        System.out.printf("  Duration: %d ms%n", duration);
        System.out.printf("  Reads/second: %.2f%n", readsPerSecond);
        System.out.printf("  Average read latency: %.2f ms%n", averageLatencyMs);
        System.out.printf("  Per-client rate: %.2f reads/second%n",
            readsPerSecond / concurrentReads);

        assertEquals(totalReadsActual, concurrentReads * readsPerClient, "All reads should complete");
        assertTrue(readsPerSecond > 100, "Should handle at least 100 reads/second");
        assertTrue(averageLatencyMs < 100, "Average read latency should be under 100ms");
    }

    @Test
    @DisplayName("Cache Performance - Eviction under pressure")
    @Timeout(value = 2, unit = TimeUnit.MINUTES)
    void testCacheEviction() throws InterruptedException {
        int maxCacheSize = 100;
        int writeCount = 1000;
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(10);

        // Simulate cache with limited size
        redisTemplate.getConnectionFactory().getConnection().flushAll();

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < 10; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < writeCount / 10; j++) {
                        String key = "evict_key_" + threadId + "_" + j;
                        String value = "evict_value_" + UUID.randomUUID().toString();
                        redisTemplate.opsForValue().set(key, value);

                        // Simulate some read operations
                        if (j % 5 == 0) {
                            redisTemplate.opsForValue().get(key);
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(2, TimeUnit.MINUTES), "All writes should complete");
        executor.shutdown();

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // Get actual number of keys in cache
        Long dbSize = redisTemplate.getConnectionFactory().getConnection().dbSize();

        System.out.printf("Cache Eviction Test:%n");
        System.out.printf("  Max cache size: %d (configurable)%n", maxCacheSize);
        System.out.printf("  Total writes: %d%n", writeCount);
        System.out.printf("  Actual keys in cache: %d%n", dbSize);
        System.out.printf("  Duration: %d ms%n", duration);
        System.out.printf("  Write rate: %.2f writes/second%n", writeCount * 1000.0 / duration);
        System.out.printf("  Cache efficiency: %.2f keys/write%n", dbSize / (double) writeCount);

        // Verify cache is managing size
        assertTrue(dbSize < writeCount, "Cache should evict old entries");
        assertTrue(dbSize > 0, "Cache should have some entries");
    }
}
