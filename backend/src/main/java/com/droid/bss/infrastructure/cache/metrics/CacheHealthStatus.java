package com.droid.bss.infrastructure.cache.metrics;

/**
 * Cache Health Status
 */
public class CacheHealthStatus {
    private final String status;
    private final String message;
    private final double hitRate;

    public CacheHealthStatus(String status, String message, double hitRate) {
        this.status = status;
        this.message = message;
        this.hitRate = hitRate;
    }

    public String getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public double getHitRate() {
        return hitRate;
    }
}
