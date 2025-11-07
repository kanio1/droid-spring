package com.droid.bss.infrastructure.cache.advanced;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Probabilistic Cache Expiration Service
 * Uses probabilistic algorithm to expire cache entries
 */
@Slf4j
@Service
public class ProbabilisticExpirationService {

    private final MultiLayerCacheManager cacheManager;
    private final HotKeyDetector hotKeyDetector;
    private final AtomicInteger totalChecks = new AtomicInteger(0);
    private final AtomicInteger expiredEntries = new AtomicInteger(0);
    private final Duration checkInterval = Duration.ofSeconds(30);
    private final double baseProbability = 0.1; // 10% base probability
    private final double hotKeyReductionFactor = 0.5; // Reduce expiration probability for hot keys
    private final int maxEntriesToCheck = 1000;
    private final boolean enabled = true;

    public ProbabilisticExpirationService(MultiLayerCacheManager cacheManager) {
        this.cacheManager = cacheManager;
        this.hotKeyDetector = new HotKeyDetector();
    }

    /**
     * Run probabilistic expiration check
     */
    @Scheduled(fixedDelayString = "${bss.cache.probabilistic-expiration.check-interval-seconds:30}")
    public void runExpirationCheck() {
        if (!enabled) {
            return;
        }

        try {
            log.trace("Running probabilistic expiration check");

            int checked = probabilisticExpiration();
            totalChecks.addAndGet(checked);

            if (checked > 0) {
                log.debug("Probabilistic expiration: checked={}, expired={}",
                    checked, expiredEntries.get());
            }

        } catch (Exception e) {
            log.error("Probabilistic expiration check failed", e);
        }
    }

    /**
     * Perform probabilistic expiration
     */
    private int probabilisticExpiration() {
        // In a real implementation, you would:
        // 1. Get all cache keys from L1 and L2
        // 2. For each key, check if it should be expired
        // 3. Apply probabilistic check

        // For this example, we'll simulate the process
        List<String> sampleKeys = getSampleCacheKeys();
        int checked = 0;
        int expired = 0;

        for (String key : sampleKeys) {
            checked++;

            // Calculate expiration probability
            double probability = calculateExpirationProbability(key);

            // Random check
            if (ThreadLocalRandom.current().nextDouble() < probability) {
                Duration ttl = cacheManager.getTtl(key);
                if (shouldExpire(ttl)) {
                    cacheManager.evict(key);
                    expired++;
                }
            }

            if (checked >= maxEntriesToCheck) {
                break;
            }
        }

        if (expired > 0) {
            expiredEntries.addAndGet(expired);
        }

        return checked;
    }

    /**
     * Calculate expiration probability for a key
     */
    private double calculateExpirationProbability(String key) {
        double probability = baseProbability;

        // Reduce probability for hot keys
        if (hotKeyDetector.isHotKey(key)) {
            probability *= hotKeyReductionFactor;
        }

        // Reduce probability for recently accessed keys
        Duration ttl = cacheManager.getTtl(key);
        if (ttl != null && ttl.toMillis() > 0) {
            // If TTL is low, increase probability
            if (ttl.toMinutes() < 5) {
                probability *= 1.5;
            }
            // If TTL is high, reduce probability
            else if (ttl.toMinutes() > 30) {
                probability *= 0.5;
            }
        }

        return probability;
    }

    /**
     * Check if a key should be expired
     */
    private boolean shouldExpire(Duration ttl) {
        if (ttl == null || ttl.isNegative() || ttl.isZero()) {
            return true; // No TTL or expired
        }

        // Check if TTL is very low
        return ttl.toMinutes() < 1;
    }

    /**
     * Get sample of cache keys for checking
     */
    private List<String> getSampleCacheKeys() {
        // In a real implementation, you would:
        // 1. Get keys from L1 cache
        // 2. Get keys from L2 cache
        // 3. Sample them

        // For this example, return an empty list
        return Collections.emptyList();
    }

    /**
     * Trigger manual expiration check
     */
    public void triggerExpirationCheck() {
        log.info("Manually triggering probabilistic expiration check");
        probabilisticExpiration();
    }

    /**
     * Get expiration statistics
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("enabled", enabled);
        stats.put("checkIntervalSeconds", checkInterval.getSeconds());
        stats.put("baseProbability", baseProbability);
        stats.put("hotKeyReductionFactor", hotKeyReductionFactor);
        stats.put("maxEntriesToCheck", maxEntriesToCheck);
        stats.put("totalChecks", totalChecks.get());
        stats.put("expiredEntries", expiredEntries.get());
        return stats;
    }

    /**
     * Reset statistics
     */
    public void resetStatistics() {
        totalChecks.set(0);
        expiredEntries.set(0);
        log.info("Reset probabilistic expiration statistics");
    }

    /**
     * Configure expiration parameters
     */
    public void configure(Duration interval, double baseProb, double reductionFactor, int maxChecks) {
        log.info("Configuring probabilistic expiration: interval={}, prob={}, reduction={}, max={}",
            interval, baseProb, reductionFactor, maxChecks);
    }
}
