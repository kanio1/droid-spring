package com.droid.bss.infrastructure.cache;

import java.util.Collection;

/**
 * Cache Statistics for monitoring and metrics
 */
public class CacheStatistics {

    private final String cacheManager;
    private final Collection<String> cacheNames;
    private final long totalEntries;
    private final double hitRate;
    private final long memoryUsageBytes;

    private CacheStatistics(Builder builder) {
        this.cacheManager = builder.cacheManager;
        this.cacheNames = builder.cacheNames;
        this.totalEntries = builder.totalEntries;
        this.hitRate = builder.hitRate;
        this.memoryUsageBytes = builder.memoryUsageBytes;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getCacheManager() {
        return cacheManager;
    }

    public Collection<String> getCacheNames() {
        return cacheNames;
    }

    public long getTotalEntries() {
        return totalEntries;
    }

    public double getHitRate() {
        return hitRate;
    }

    public long getMemoryUsageBytes() {
        return memoryUsageBytes;
    }

    public static class Builder {
        private String cacheManager;
        private Collection<String> cacheNames;
        private long totalEntries;
        private double hitRate;
        private long memoryUsageBytes;

        public Builder cacheManager(String cacheManager) {
            this.cacheManager = cacheManager;
            return this;
        }

        public Builder cacheNames(Collection<String> cacheNames) {
            this.cacheNames = cacheNames;
            return this;
        }

        public Builder totalEntries(long totalEntries) {
            this.totalEntries = totalEntries;
            return this;
        }

        public Builder hitRate(double hitRate) {
            this.hitRate = hitRate;
            return this;
        }

        public Builder memoryUsageBytes(long memoryUsageBytes) {
            this.memoryUsageBytes = memoryUsageBytes;
            return this;
        }

        public CacheStatistics build() {
            return new CacheStatistics(this);
        }
    }

    @Override
    public String toString() {
        return "CacheStatistics{" +
                "cacheManager='" + cacheManager + '\'' +
                ", cacheNames=" + cacheNames +
                ", totalEntries=" + totalEntries +
                ", hitRate=" + String.format("%.2f%%", hitRate * 100) +
                ", memoryUsageBytes=" + memoryUsageBytes +
                '}';
    }
}
