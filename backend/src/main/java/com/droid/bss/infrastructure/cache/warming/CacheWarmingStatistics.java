package com.droid.bss.infrastructure.cache.warming;

/**
 * Cache Warming Statistics
 */
public class CacheWarmingStatistics {
    private final String cacheType;
    private final long lastWarmedAt;
    private final int recordsWarmed;
    private final CacheWarmingStatus status;
    private final long warmingDurationMs;

    public CacheWarmingStatistics(String cacheType, long lastWarmedAt,
                                 int recordsWarmed, CacheWarmingStatus status) {
        this.cacheType = cacheType;
        this.lastWarmedAt = lastWarmedAt;
        this.recordsWarmed = recordsWarmed;
        this.status = status;
        this.warmingDurationMs = 0; // Could be calculated if needed
    }

    public String getCacheType() {
        return cacheType;
    }

    public long getLastWarmedAt() {
        return lastWarmedAt;
    }

    public int getRecordsWarmed() {
        return recordsWarmed;
    }

    public CacheWarmingStatus getStatus() {
        return status;
    }

    public long getWarmingDurationMs() {
        return warmingDurationMs;
    }

    public String getLastWarmedFormatted() {
        return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                .format(new java.util.Date(lastWarmedAt));
    }
}
