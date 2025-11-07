package com.droid.bss.infrastructure.cache.metrics;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Cache Statistics
 *
 * Tracks statistics for a single cache
 */
public class CacheStatistics {
    private final String cacheName;
    private final AtomicLong hits = new AtomicLong(0);
    private final AtomicLong misses = new AtomicLong(0);
    private final AtomicLong evictions = new AtomicLong(0);
    private final AtomicLong puts = new AtomicLong(0);
    private final AtomicLong lastAccessTime = new AtomicLong(System.currentTimeMillis());

    public CacheStatistics(String cacheName) {
        this.cacheName = cacheName;
    }

    public synchronized void recordHit() {
        hits.incrementAndGet();
        lastAccessTime.set(System.currentTimeMillis());
    }

    public synchronized void recordMiss() {
        misses.incrementAndGet();
        lastAccessTime.set(System.currentTimeMillis());
    }

    public synchronized void recordEviction() {
        evictions.incrementAndGet();
    }

    public synchronized void recordPut() {
        puts.incrementAndGet();
        lastAccessTime.set(System.currentTimeMillis());
    }

    public String getCacheName() {
        return cacheName;
    }

    public long getHits() {
        return hits.get();
    }

    public long getMisses() {
        return misses.get();
    }

    public long getEvictions() {
        return evictions.get();
    }

    public long getPuts() {
        return puts.get();
    }

    public long getLastAccessTime() {
        return lastAccessTime.get();
    }

    public double getHitRate() {
        long total = hits.get() + misses.get();
        if (total == 0) {
            return 0.0;
        }
        return (double) hits.get() / total * 100.0;
    }

    public boolean isActive() {
        long idleTime = System.currentTimeMillis() - lastAccessTime.get();
        return idleTime < 300000; // 5 minutes
    }
}
