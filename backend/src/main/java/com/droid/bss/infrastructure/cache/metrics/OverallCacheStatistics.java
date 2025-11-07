package com.droid.bss.infrastructure.cache.metrics;

/**
 * Overall Cache Statistics
 */
public class OverallCacheStatistics {
    private final long totalHits;
    private final long totalMisses;
    private final long totalEvictions;
    private final long totalPuts;
    private final int numberOfCaches;

    public OverallCacheStatistics(long totalHits, long totalMisses,
                                 long totalEvictions, long totalPuts,
                                 int numberOfCaches) {
        this.totalHits = totalHits;
        this.totalMisses = totalMisses;
        this.totalEvictions = totalEvictions;
        this.totalPuts = totalPuts;
        this.numberOfCaches = numberOfCaches;
    }

    public long getTotalHits() {
        return totalHits;
    }

    public long getTotalMisses() {
        return totalMisses;
    }

    public long getTotalEvictions() {
        return totalEvictions;
    }

    public long getTotalPuts() {
        return totalPuts;
    }

    public int getNumberOfCaches() {
        return numberOfCaches;
    }

    public long getTotalRequests() {
        return totalHits + totalMisses;
    }

    public double getHitRate() {
        long total = getTotalRequests();
        if (total == 0) {
            return 0.0;
        }
        return (double) totalHits / total * 100.0;
    }
}
