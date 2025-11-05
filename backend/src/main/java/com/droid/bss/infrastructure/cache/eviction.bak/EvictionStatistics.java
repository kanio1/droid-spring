package com.droid.bss.infrastructure.cache.eviction;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Statistics for cache eviction operations.
 *
 * @since 1.0
 */
public class EvictionStatistics {

    private final Instant createdAt;
    private final AtomicLong totalEvictions = new AtomicLong(0);
    private final AtomicLong totalExpiredEvictions = new AtomicLong(0);
    private final AtomicLong totalIdleEvictions = new AtomicLong(0);
    private final AtomicLong totalSizeEvictions = new AtomicLong(0);
    private final AtomicLong totalPolicyEvictions = new AtomicLong(0);
    private final AtomicLong lastEvictionTime = new AtomicLong(0);

    /**
     * Creates a new EvictionStatistics.
     */
    public EvictionStatistics() {
        this.createdAt = Instant.now();
    }

    /**
     * Records an eviction.
     */
    public void recordEviction() {
        totalEvictions.incrementAndGet();
        lastEvictionTime.set(System.currentTimeMillis());
    }

    /**
     * Records an expired entry eviction.
     */
    public void recordExpiredEviction() {
        totalExpiredEvictions.incrementAndGet();
        recordEviction();
    }

    /**
     * Records an idle entry eviction.
     */
    public void recordIdleEviction() {
        totalIdleEvictions.incrementAndGet();
        recordEviction();
    }

    /**
     * Records a size-based eviction.
     */
    public void recordSizeEviction() {
        totalSizeEvictions.incrementAndGet();
        recordEviction();
    }

    /**
     * Records a policy-based eviction.
     */
    public void recordPolicyEviction() {
        totalPolicyEvictions.incrementAndGet();
        recordEviction();
    }

    /**
     * Gets the total number of evictions.
     *
     * @return the total evictions
     */
    public long getTotalEvictions() {
        return totalEvictions.get();
    }

    /**
     * Gets the number of expired entry evictions.
     *
     * @return the expired evictions
     */
    public long getTotalExpiredEvictions() {
        return totalExpiredEvictions.get();
    }

    /**
     * Gets the number of idle entry evictions.
     *
     * @return the idle evictions
     */
    public long getTotalIdleEvictions() {
        return totalIdleEvictions.get();
    }

    /**
     * Gets the number of size-based evictions.
     *
     * @return the size evictions
     */
    public long getTotalSizeEvictions() {
        return totalSizeEvictions.get();
    }

    /**
     * Gets the number of policy-based evictions.
     *
     * @return the policy evictions
     */
    public long getTotalPolicyEvictions() {
        return totalPolicyEvictions.get();
    }

    /**
     * Gets the last eviction time.
     *
     * @return the last eviction time (epoch millis)
     */
    public long getLastEvictionTime() {
        return lastEvictionTime.get();
    }

    /**
     * Gets the statistics age in milliseconds.
     *
     * @return the age in milliseconds
     */
    public long getAgeMs() {
        return java.time.Duration.between(createdAt, Instant.now()).toMillis();
    }

    /**
     * Resets all statistics.
     */
    public void reset() {
        totalEvictions.set(0);
        totalExpiredEvictions.set(0);
        totalIdleEvictions.set(0);
        totalSizeEvictions.set(0);
        totalPolicyEvictions.set(0);
        lastEvictionTime.set(0);
    }

    /**
     * Gets a summary of the statistics.
     *
     * @return the summary
     */
    public String getSummary() {
        return String.format(
            "Evictions: total=%d, expired=%d, idle=%d, size=%d, policy=%d, lastEviction=%dms ago",
            getTotalEvictions(),
            getTotalExpiredEvictions(),
            getTotalIdleEvictions(),
            getTotalSizeEvictions(),
            getTotalPolicyEvictions(),
            getLastEvictionTime() > 0 ? (System.currentTimeMillis() - getLastEvictionTime()) : -1
        );
    }

    @Override
    public String toString() {
        return "EvictionStatistics{" +
            "totalEvictions=" + getTotalEvictions() +
            ", totalExpiredEvictions=" + getTotalExpiredEvictions() +
            ", totalIdleEvictions=" + getTotalIdleEvictions() +
            ", totalSizeEvictions=" + getTotalSizeEvictions() +
            ", totalPolicyEvictions=" + getTotalPolicyEvictions() +
            ", ageMs=" + getAgeMs() +
            '}';
    }
}
