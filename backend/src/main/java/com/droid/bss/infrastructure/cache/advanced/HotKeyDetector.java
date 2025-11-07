package com.droid.bss.infrastructure.cache.advanced;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Hot Key Detector
 * Identifies frequently accessed cache keys
 */
@Slf4j
@Component
public class HotKeyDetector {

    private final Map<String, AtomicInteger> accessCounts = new ConcurrentHashMap<>();
    private final Set<String> hotKeys = new HashSet<>();
    private final int hotKeyThreshold = 20; // Access count threshold
    private final int hotKeyWindowMinutes = 10;

    /**
     * Record key access
     */
    public void recordAccess(String key) {
        accessCounts.computeIfAbsent(key, k -> new AtomicInteger(0)).incrementAndGet();
    }

    /**
     * Detect hot keys
     */
    public void detectHotKeys() {
        log.debug("Detecting hot keys");

        Set<String> detectedHotKeys = accessCounts.entrySet().stream()
            .filter(entry -> entry.getValue().get() >= hotKeyThreshold)
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());

        // Update hot keys set
        hotKeys.addAll(detectedHotKeys);

        log.info("Detected {} hot keys", hotKeys.size());
    }

    /**
     * Get hot keys
     */
    public Set<String> getHotKeys() {
        return new HashSet<>(hotKeys);
    }

    /**
     * Check if a key is hot
     */
    public boolean isHotKey(String key) {
        return hotKeys.contains(key) || accessCounts.getOrDefault(key, new AtomicInteger(0)).get() >= hotKeyThreshold;
    }

    /**
     * Get access count for a key
     */
    public int getAccessCount(String key) {
        return accessCounts.getOrDefault(key, new AtomicInteger(0)).get();
    }

    /**
     * Get top N hot keys
     */
    public List<String> getTopHotKeys(int limit) {
        return accessCounts.entrySet().stream()
            .sorted(Map.Entry.<String, AtomicInteger>comparingByValue().reversed())
            .limit(limit)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }

    /**
     * Clear access counts
     */
    public void clearAccessCounts() {
        accessCounts.clear();
        hotKeys.clear();
        log.info("Cleared hot key detector data");
    }

    /**
     * Configure hot key threshold
     */
    public void setHotKeyThreshold(int threshold) {
        log.info("Updated hot key threshold: {}", threshold);
    }

    /**
     * Get detector statistics
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalKeys", accessCounts.size());
        stats.put("hotKeys", hotKeys.size());
        stats.put("threshold", hotKeyThreshold);
        stats.put("topKeys", getTopHotKeys(5));
        return stats;
    }
}
