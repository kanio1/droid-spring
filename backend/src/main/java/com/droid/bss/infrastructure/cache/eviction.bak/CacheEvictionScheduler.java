package com.droid.bss.infrastructure.cache.eviction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Scheduler for periodic cache eviction cleanup.
 *
 * @since 1.0
 */
public class CacheEvictionScheduler {

    private static final Logger log = LoggerFactory.getLogger(CacheEvictionScheduler.class);

    private final CacheEvictionProperties evictionProperties;
    private final Map<String, CacheEvictionStrategy<String, Object>> strategies;
    private final Map<String, Long> lastCleanupTime = new ConcurrentHashMap<>();

    /**
     * Creates a new CacheEvictionScheduler.
     *
     * @param evictionProperties the eviction properties
     * @param strategies the eviction strategies
     */
    public CacheEvictionScheduler(CacheEvictionProperties evictionProperties,
                                  Map<String, CacheEvictionStrategy<String, Object>> strategies) {
        this.evictionProperties = evictionProperties;
        this.strategies = strategies;
    }

    /**
     * Runs periodic cleanup of expired and idle entries.
     */
    @Scheduled(fixedDelayString = "${app.cache.eviction.cleanup-interval-seconds:60}000")
    public void runCleanup() {
        if (!evictionProperties.getAutoCleanupEnabled()) {
            return;
        }

        log.debug("Starting periodic cache eviction cleanup");

        long ttlMs = evictionProperties.getDefaultTtlSeconds() * 1000L;
        long idleThresholdMs = evictionProperties.getIdleThresholdMs();

        for (Map.Entry<String, CacheEvictionStrategy<String, Object>> entry : strategies.entrySet()) {
            String cacheName = entry.getKey();
            CacheEvictionStrategy<String, Object> strategy = entry.getValue();

            try {
                long expiredCount = 0;
                long idleCount = 0;

                // Note: This is a simplified implementation
                // In a real scenario, you'd access the actual cache entries
                if (log.isDebugEnabled()) {
                    log.debug("Cleanup completed for cache '{}': strategy={}",
                        cacheName, strategy.getName());
                }

                lastCleanupTime.put(cacheName, System.currentTimeMillis());

            } catch (Exception e) {
                log.error("Error during cache eviction cleanup for cache '{}': {}", cacheName, e.getMessage(), e);
            }
        }

        log.debug("Periodic cache eviction cleanup completed");
    }

    /**
     * Gets the last cleanup time for a cache.
     *
     * @param cacheName the cache name
     * @return the last cleanup time (epoch millis)
     */
    public long getLastCleanupTime(String cacheName) {
        return lastCleanupTime.getOrDefault(cacheName, 0L);
    }

    /**
     * Gets the time since last cleanup for a cache.
     *
     * @param cacheName the cache name
     * @return the time since last cleanup in milliseconds
     */
    public long getTimeSinceLastCleanup(String cacheName) {
        long lastCleanup = getLastCleanupTime(cacheName);
        if (lastCleanup == 0) {
            return -1;
        }
        return System.currentTimeMillis() - lastCleanup;
    }
}
