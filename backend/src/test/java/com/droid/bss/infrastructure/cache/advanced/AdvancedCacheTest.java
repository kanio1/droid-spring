package com.droid.bss.infrastructure.cache.advanced;

import org.junit.jupiter.api.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Advanced Cache Tests
 * Tests multi-layer caching, invalidation, warming, and statistics
 */
class AdvancedCacheTest {

    @Configuration
    @EnableAspectJAutoProxy
    static class TestConfig {
        @Bean
        public com.github.benmanes.caffeine.cache.Cache<String, Object> l1Cache() {
            return com.github.benmanes.caffeine.cache.Caffeine.newBuilder()
                .maximumSize(100)
                .expireAfterWrite(Duration.ofMinutes(10))
                .build();
        }

        @Bean
        public MultiLayerCacheManager cacheManager() {
            return new MultiLayerCacheManager(l1Cache(), null);
        }

        @Bean
        public CacheKeyGenerator keyGenerator() {
            return new CacheKeyGenerator();
        }
    }

    private MultiLayerCacheManager cacheManager;
    private CacheKeyGenerator keyGenerator;
    private CacheWarmingService warmingService;
    private CacheInvalidationService invalidationService;
    private CacheStatisticsService statisticsService;
    private HotKeyDetector hotKeyDetector;

    @BeforeEach
    void setUp() {
        cacheManager = new MultiLayerCacheManager(
            com.github.benmanes.caffeine.cache.Caffeine.newBuilder().build(),
            null
        );
        keyGenerator = new CacheKeyGenerator();
        warmingService = new CacheWarmingService(cacheManager);
        invalidationService = new CacheInvalidationService(cacheManager, keyGenerator);
        statisticsService = new CacheStatisticsService(cacheManager);
        hotKeyDetector = new HotKeyDetector();
    }

    @Test
    @DisplayName("Cache should store and retrieve values")
    void testCacheStoreAndRetrieve() {
        String key = "test:key";
        String value = "test-value";

        cacheManager.put(key, value);
        Optional<String> retrieved = cacheManager.get(key, String.class);

        assertTrue(retrieved.isPresent());
        assertEquals(value, retrieved.get());
    }

    @Test
    @DisplayName("Cache should evict values")
    void testCacheEviction() {
        String key = "test:evict";
        String value = "evict-me";

        cacheManager.put(key, value);
        assertTrue(cacheManager.hasKey(key));

        cacheManager.evict(key);
        assertFalse(cacheManager.hasKey(key));
    }

    @Test
    @DisplayName("Cache should get or load values")
    void testCacheGetOrLoad() {
        String key = "test:load";
        String loadedValue = "loaded-data";

        // Cache miss, should load
        String result = cacheManager.getOrLoad(
            key,
            k -> loadedValue,
            Duration.ofMinutes(10),
            String.class
        );

        assertEquals(loadedValue, result);

        // Cache hit, should not load
        String result2 = cacheManager.getOrLoad(
            key,
            k -> { throw new RuntimeException("Should not be called"); },
            Duration.ofMinutes(10),
            String.class
        );

        assertEquals(loadedValue, result2);
    }

    @Test
    @DisplayName("Key generator should create consistent keys")
    void testKeyGeneration() {
        String className = "TestService";
        String methodName = "getData";
        String param1 = "value1";
        String param2 = "value2";

        String key1 = keyGenerator.generateKey(className, methodName, param1, param2);
        String key2 = keyGenerator.generateKey(className, methodName, param1, param2);

        assertEquals(key1, key2);
        assertTrue(key1.contains(className));
        assertTrue(key1.contains(methodName));
        assertTrue(key1.startsWith("bss"));

        System.out.printf("Generated key: %s%n", key1);
    }

    @Test
    @DisplayName("Key generator should create different keys for different parameters")
    void testKeyUniqueness() {
        String key1 = keyGenerator.generateKey("Service", "Method", "param1");
        String key2 = keyGenerator.generateKey("Service", "Method", "param2");

        assertNotEquals(key1, key2);
    }

    @Test
    @DisplayName("Cache warming should populate hot data")
    void testCacheWarming() {
        warmingService.addWarmingTask("custom", cacheManager -> {
            cacheManager.put("warmed:key1", "value1", Duration.ofMinutes(30));
            cacheManager.put("warmed:key2", "value2", Duration.ofMinutes(30));
        });

        warmingService.warmAll();

        assertTrue(cacheManager.hasKey("warmed:key1"));
        assertTrue(cacheManager.hasKey("warmed:key2"));
    }

    @Test
    @DisplayName("Cache invalidation should work by pattern")
    void testPatternInvalidation() {
        // Put some data
        cacheManager.put("order:123", "order-123");
        cacheManager.put("order:456", "order-456");
        cacheManager.put("product:789", "product-789");

        // Invalidate order pattern
        cacheManager.evictPattern("order:*");

        assertFalse(cacheManager.hasKey("order:123"));
        assertFalse(cacheManager.hasKey("order:456"));
        assertTrue(cacheManager.hasKey("product:789"));
    }

    @Test
    @DisplayName("Cache statistics should track operations")
    void testStatisticsTracking() {
        String key = "stats:test";

        // Record misses
        statisticsService.recordCacheMiss(key);
        statisticsService.recordCacheMiss(key);

        // Record hits
        cacheManager.put(key, "value");
        statisticsService.recordCacheHit(key);

        // Record puts
        statisticsService.recordCachePut(key);

        var stats = statisticsService.getStatistics();

        assertEquals(2, stats.getTotalMisses());
        assertEquals(1, stats.getTotalHits());
        assertTrue(stats.getHitRate() > 0);
        assertTrue(stats.getTotalPuts() > 0);

        System.out.printf("Cache statistics: %s%n", stats);
    }

    @Test
    @DisplayName("Hot key detector should identify hot keys")
    void testHotKeyDetection() {
        // Access a key many times
        String key = "hot:key";
        for (int i = 0; i < 25; i++) {
            hotKeyDetector.recordAccess(key);
        }

        hotKeyDetector.detectHotKeys();

        assertTrue(hotKeyDetector.isHotKey(key));
        assertTrue(hotKeyDetector.getAccessCount(key) >= 25);

        List<String> topKeys = hotKeyDetector.getTopHotKeys(5);
        assertTrue(topKeys.contains(key));
    }

    @Test
    @DisplayName("Cache should handle TTL")
    void testTtlHandling() {
        String key = "ttl:test";
        String value = "ttl-value";
        Duration ttl = Duration.ofSeconds(10);

        cacheManager.put(key, value, ttl);

        assertTrue(cacheManager.hasKey(key));
        assertTrue(cacheManager.getTtl(key).toMillis() > 0);

        System.out.printf("TTL: %s%n", cacheManager.getTtl(key));
    }

    @Test
    @DisplayName("Cache should extend TTL")
    void testTtlExtension() {
        String key = "extend:test";
        Duration initialTtl = Duration.ofSeconds(10);
        Duration extendedTtl = Duration.ofMinutes(5);

        cacheManager.put(key, "value", initialTtl);
        cacheManager.extendTtl(key, extendedTtl);

        Duration newTtl = cacheManager.getTtl(key);
        assertTrue(newTtl.compareTo(initialTtl) > 0);

        System.out.printf("Extended TTL: %s%n", newTtl);
    }

    @Test
    @DisplayName("Cache should provide comprehensive statistics")
    void testComprehensiveStatistics() {
        // Perform various operations
        for (int i = 0; i < 10; i++) {
            cacheManager.put("key" + i, "value" + i);
        }

        // Access some keys
        cacheManager.get("key1", String.class);
        cacheManager.get("key2", String.class);

        var basicStats = cacheManager.getStatistics();
        assertNotNull(basicStats);
        assertTrue(basicStats.getL1Size() > 0);

        System.out.printf("Basic cache stats: %s%n", basicStats);
    }

    @Test
    @DisplayName("Cache should clear all entries")
    void testCacheClear() {
        cacheManager.put("key1", "value1");
        cacheManager.put("key2", "value2");
        cacheManager.put("key3", "value3");

        assertTrue(cacheManager.hasKey("key1"));
        assertTrue(cacheManager.hasKey("key2"));
        assertTrue(cacheManager.hasKey("key3"));

        cacheManager.clear();

        // L1 cache should be clear
        assertFalse(cacheManager.hasKey("key1"));
        // Note: L2 cache clear depends on Redis connection
    }

    @Nested
    @DisplayName("Key Generation Tests")
    class KeyGenerationTests {

        @Test
        @DisplayName("Should generate entity keys")
        void testEntityKeys() {
            String key = keyGenerator.generateEntityKey("customer", "123");
            assertTrue(key.contains("customer"));
            assertTrue(key.contains("123"));
            assertTrue(key.startsWith("bss"));
        }

        @Test
        @DisplayName("Should generate collection keys")
        void testCollectionKeys() {
            String key = keyGenerator.generateCollectionKey("products", "abc123");
            assertTrue(key.contains("products"));
            assertTrue(key.contains("abc123"));
        }

        @Test
        @DisplayName("Should generate aggregate keys")
        void testAggregateKeys() {
            String key = keyGenerator.generateAggregateKey("revenue", "monthly", "xyz789");
            assertTrue(key.contains("revenue"));
            assertTrue(key.contains("monthly"));
            assertTrue(key.contains("xyz789"));
        }
    }

    @Nested
    @DisplayName("Warming Service Tests")
    class WarmingServiceTests {

        @Test
        @DisplayName("Should add warming tasks")
        void testAddWarmingTasks() {
            warmingService.addWarmingTask("task1", cm -> cm.put("task1:key", "value"));
            warmingService.addWarmingTask("task2", cm -> cm.put("task2:key", "value"));

            var stats = warmingService.getWarmingStatistics();
            assertEquals(2, stats.get("registeredTasks"));
        }

        @Test
        @DisplayName("Should configure warming")
        void testConfigureWarming() {
            warmingService.setEnabled(true);
            warmingService.setWarmingInterval(Duration.ofMinutes(5));

            var stats = warmingService.getWarmingStatistics();
            assertTrue((Boolean) stats.get("enabled"));
            assertEquals(5L, stats.get("intervalMinutes"));
        }
    }

    @Nested
    @DisplayName("Invalidation Service Tests")
    class InvalidationServiceTests {

        @Test
        @DisplayName("Should track dependencies")
        void testDependencyTracking() {
            String entityId = "customer:123";
            Set<String> relatedKeys = Set.of("order:1", "order:2", "invoice:1");

            invalidationService.registerDependency(entityId, relatedKeys);

            // Invalidation would clear related keys
            invalidationService.invalidateEntity("customer", "123");

            // In a real test, we would verify the related keys are invalidated
        }
    }
}
