package com.droid.bss.infrastructure.cache.eviction;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Entry in a cache with metadata for eviction policies.
 *
 * @since 1.0
 */
public class CacheEntry<V> {

    private final V value;
    private final Instant createdAt;
    private volatile Instant lastAccessedAt;
    private final AtomicLong accessCount = new AtomicLong(0);
    private final AtomicLong size = new AtomicLong(1);

    /**
     * Creates a new CacheEntry.
     *
     * @param value the cached value
     */
    public CacheEntry(V value) {
        this(value, 1);
    }

    /**
     * Creates a new CacheEntry with custom size.
     *
     * @param value the cached value
     * @param size the entry size
     */
    public CacheEntry(V value, long size) {
        this.value = value;
        this.createdAt = Instant.now();
        this.lastAccessedAt = this.createdAt;
        this.size.set(size);
    }

    /**
     * Gets the cached value.
     *
     * @return the value
     */
    public V getValue() {
        return value;
    }

    /**
     * Gets the creation time.
     *
     * @return the creation time
     */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /**
     * Gets the last accessed time.
     *
     * @return the last accessed time
     */
    public Instant getLastAccessedAt() {
        return lastAccessedAt;
    }

    /**
     * Gets the access count.
     *
     * @return the access count
     */
    public long getAccessCount() {
        return accessCount.get();
    }

    /**
     * Gets the entry size.
     *
     * @return the size
     */
    public long getSize() {
        return size.get();
    }

    /**
     * Records an access to this entry.
     */
    public void recordAccess() {
        lastAccessedAt = Instant.now();
        accessCount.incrementAndGet();
    }

    /**
     * Sets the entry size.
     *
     * @param newSize the new size
     */
    public void setSize(long newSize) {
        if (newSize > 0) {
            size.set(newSize);
        }
    }

    /**
     * Gets the age of the entry in milliseconds.
     *
     * @return the age in milliseconds
     */
    public long getAgeMs() {
        return java.time.Duration.between(createdAt, Instant.now()).toMillis();
    }

    /**
     * Gets the idle time in milliseconds.
     *
     * @return the idle time in milliseconds
     */
    public long getIdleTimeMs() {
        return java.time.Duration.between(lastAccessedAt, Instant.now()).toMillis();
    }

    /**
     * Checks if the entry has expired based on TTL.
     *
     * @param ttlMs the time-to-live in milliseconds
     * @return true if expired
     */
    public boolean isExpired(long ttlMs) {
        if (ttlMs <= 0) {
            return false;
        }
        return getAgeMs() > ttlMs;
    }

    /**
     * Checks if the entry is idle for longer than the threshold.
     *
     * @param idleThresholdMs the idle threshold in milliseconds
     * @return true if idle
     */
    public boolean isIdle(long idleThresholdMs) {
        if (idleThresholdMs <= 0) {
            return false;
        }
        return getIdleTimeMs() > idleThresholdMs;
    }

    @Override
    public String toString() {
        return "CacheEntry{" +
            "value=" + value +
            ", createdAt=" + createdAt +
            ", lastAccessedAt=" + lastAccessedAt +
            ", accessCount=" + accessCount.get() +
            ", size=" + size.get() +
            ", ageMs=" + getAgeMs() +
            ", idleTimeMs=" + getIdleTimeMs() +
            '}';
    }
}
