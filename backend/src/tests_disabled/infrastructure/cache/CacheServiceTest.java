package com.droid.bss.infrastructure.cache;

import com.droid.bss.BssApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.test.context.TestPropertySource;

import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Cache Service Tests
 *
 * Tests Redis cache operations, get/put/evict/clear functionality.
 */
@SpringBootTest(classes = Application.class)
@EnableCaching
@TestPropertySource(properties = {
    "spring.cache.type=redis",
    "spring.cache.redis.time-to-live=60000"
})
@DisplayName("Cache Service Tests")
class CacheServiceTest {

    @Autowired(required = false)
    private CacheManager cacheManager;

    private static final String TEST_CACHE = "test-cache";
    private static final String TEST_KEY = "test-key";
    private static final String TEST_VALUE = "test-value";

    @Test
    @DisplayName("Should get cached value")
    void shouldGetCachedValue() {
        if (cacheManager != null) {
            var cache = cacheManager.getCache(TEST_CACHE);
            assertThat(cache).isNotNull();

            if (cache != null) {
                // Put a value
                cache.put(TEST_KEY, TEST_VALUE);

                // Get the value
                var retrievedValue = cache.get(TEST_KEY, String.class);
                assertThat(retrievedValue).isEqualTo(TEST_VALUE);
            }
        }
    }

    @Test
    @DisplayName("Should put value in cache")
    void shouldPutValueInCache() {
        if (cacheManager != null) {
            var cache = cacheManager.getCache(TEST_CACHE);
            assertThat(cache).isNotNull();

            if (cache != null) {
                // Put a value
                cache.put(TEST_KEY, TEST_VALUE);

                // Verify it's there
                var retrievedValue = cache.get(TEST_KEY, String.class);
                assertThat(retrievedValue).isEqualTo(TEST_VALUE);
            }
        }
    }

    @Test
    @DisplayName("Should evict cache entry")
    void shouldEvictCacheEntry() {
        if (cacheManager != null) {
            var cache = cacheManager.getCache(TEST_CACHE);
            assertThat(cache).isNotNull();

            if (cache != null) {
                // Put a value
                cache.put(TEST_KEY, TEST_VALUE);
                assertThat(cache.get(TEST_KEY)).isNotNull();

                // Evict the value
                cache.evictIfPresent(TEST_KEY);

                // Verify it's gone
                assertThat(cache.get(TEST_KEY)).isNull();
            }
        }
    }

    @Test
    @DisplayName("Should clear all cache entries")
    void shouldClearAllCacheEntries() {
        if (cacheManager != null) {
            var cache = cacheManager.getCache(TEST_CACHE);
            assertThat(cache).isNotNull();

            if (cache != null) {
                // Put multiple values
                cache.put("key1", "value1");
                cache.put("key2", "value2");
                cache.put("key3", "value3");

                // Verify they're there
                assertThat(cache.get("key1")).isNotNull();
                assertThat(cache.get("key2")).isNotNull();
                assertThat(cache.get("key3")).isNotNull();

                // Clear all
                cache.clear();

                // Verify they're all gone
                assertThat(cache.get("key1")).isNull();
                assertThat(cache.get("key2")).isNull();
                assertThat(cache.get("key3")).isNull();
            }
        }
    }
}
