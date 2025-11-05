package com.droid.bss.infrastructure.database.pooling;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Statistics for a connection pool.
 *
 * @since 1.0
 */
public class ConnectionPoolStats {

    private final Instant createdAt;
    private final String poolName;
    private final AtomicLong totalConnectionsCreated = new AtomicLong(0);
    private final AtomicLong totalConnectionsDestroyed = new AtomicLong(0);
    private final AtomicLong totalConnectionsRequested = new AtomicLong(0);
    private final AtomicLong totalConnectionsServed = new AtomicLong(0);
    private final AtomicLong totalConnectionTimeouts = new AtomicLong(0);
    private final AtomicLong totalValidationFailures = new AtomicLong(0);
    private final AtomicLong totalIdleTimeMs = new AtomicLong(0);
    private final AtomicLong totalActiveTimeMs = new AtomicLong(0);
    private final AtomicLong peakActiveConnections = new AtomicLong(0);
    private final AtomicLong peakIdleConnections = new AtomicLong(0);

    // Current metrics
    private volatile int currentActiveConnections = 0;
    private volatile int currentIdleConnections = 0;
    private volatile int currentPendingConnections = 0;

    /**
     * Creates a new ConnectionPoolStats.
     *
     * @param poolName the pool name
     */
    public ConnectionPoolStats(String poolName) {
        this.poolName = poolName;
        this.createdAt = Instant.now();
    }

    /**
     * Records a connection creation.
     */
    public void recordConnectionCreated() {
        totalConnectionsCreated.incrementAndGet();
    }

    /**
     * Records a connection destruction.
     */
    public void recordConnectionDestroyed() {
        totalConnectionsDestroyed.incrementAndGet();
    }

    /**
     * Records a connection request.
     */
    public void recordConnectionRequested() {
        totalConnectionsRequested.incrementAndGet();
    }

    /**
     * Records a connection being served.
     */
    public void recordConnectionServed() {
        totalConnectionsServed.incrementAndGet();
    }

    /**
     * Records a connection timeout.
     */
    public void recordConnectionTimeout() {
        totalConnectionTimeouts.incrementAndGet();
    }

    /**
     * Records a validation failure.
     */
    public void recordValidationFailure() {
        totalValidationFailures.incrementAndGet();
    }

    /**
     * Records idle time.
     *
     * @param idleTimeMs the idle time in milliseconds
     */
    public void recordIdleTime(long idleTimeMs) {
        totalIdleTimeMs.addAndGet(idleTimeMs);
    }

    /**
     * Records active time.
     *
     * @param activeTimeMs the active time in milliseconds
     */
    public void recordActiveTime(long activeTimeMs) {
        totalActiveTimeMs.addAndGet(activeTimeMs);
    }

    /**
     * Updates the peak active connections count.
     *
     * @param currentActive the current active connections
     */
    public void updatePeakActive(int currentActive) {
        peakActiveConnections.updateAndGet(current -> Math.max(current, currentActive));
    }

    /**
     * Updates the peak idle connections count.
     *
     * @param currentIdle the current idle connections
     */
    public void updatePeakIdle(int currentIdle) {
        peakIdleConnections.updateAndGet(current -> Math.max(current, currentIdle));
    }

    /**
     * Sets the current active connections count.
     *
     * @param count the current active connections
     */
    public void setCurrentActiveConnections(int count) {
        this.currentActiveConnections = count;
        updatePeakActive(count);
    }

    /**
     * Sets the current idle connections count.
     *
     * @param count the current idle connections
     */
    public void setCurrentIdleConnections(int count) {
        this.currentIdleConnections = count;
        updatePeakIdle(count);
    }

    /**
     * Sets the current pending connections count.
     *
     * @param count the current pending connections
     */
    public void setCurrentPendingConnections(int count) {
        this.currentPendingConnections = count;
    }

    /**
     * Gets the total connections created.
     *
     * @return the total created
     */
    public long getTotalConnectionsCreated() {
        return totalConnectionsCreated.get();
    }

    /**
     * Gets the total connections destroyed.
     *
     * @return the total destroyed
     */
    public long getTotalConnectionsDestroyed() {
        return totalConnectionsDestroyed.get();
    }

    /**
     * Gets the total connections requested.
     *
     * @return the total requested
     */
    public long getTotalConnectionsRequested() {
        return totalConnectionsRequested.get();
    }

    /**
     * Gets the total connections served.
     *
     * @return the total served
     */
    public long getTotalConnectionsServed() {
        return totalConnectionsServed.get();
    }

    /**
     * Gets the total connection timeouts.
     *
     * @return the total timeouts
     */
    public long getTotalConnectionTimeouts() {
        return totalConnectionTimeouts.get();
    }

    /**
     * Gets the total validation failures.
     *
     * @return the total validation failures
     */
    public long getTotalValidationFailures() {
        return totalValidationFailures.get();
    }

    /**
     * Gets the total idle time in milliseconds.
     *
     * @return the total idle time
     */
    public long getTotalIdleTimeMs() {
        return totalIdleTimeMs.get();
    }

    /**
     * Gets the total active time in milliseconds.
     *
     * @return the total active time
     */
    public long getTotalActiveTimeMs() {
        return totalActiveTimeMs.get();
    }

    /**
     * Gets the peak active connections.
     *
     * @return the peak active connections
     */
    public long getPeakActiveConnections() {
        return peakActiveConnections.get();
    }

    /**
     * Gets the peak idle connections.
     *
     * @return the peak idle connections
     */
    public long getPeakIdleConnections() {
        return peakIdleConnections.get();
    }

    /**
     * Gets the current active connections.
     *
     * @return the current active connections
     */
    public int getCurrentActiveConnections() {
        return currentActiveConnections;
    }

    /**
     * Gets the current idle connections.
     *
     * @return the current idle connections
     */
    public int getCurrentIdleConnections() {
        return currentIdleConnections;
    }

    /**
     * Gets the current pending connections.
     *
     * @return the current pending connections
     */
    public int getCurrentPendingConnections() {
        return currentPendingConnections;
    }

    /**
     * Gets the pool age in milliseconds.
     *
     * @return the age in milliseconds
     */
    public long getPoolAgeMs() {
        return java.time.Duration.between(createdAt, Instant.now()).toMillis();
    }

    /**
     * Gets the average connection lifetime in milliseconds.
     *
     * @return the average lifetime (0 if no connections destroyed)
     */
    public double getAverageConnectionLifetimeMs() {
        long destroyed = totalConnectionsDestroyed.get();
        if (destroyed == 0) {
            return 0;
        }
        return (double) (totalActiveTimeMs.get() + totalIdleTimeMs.get()) / destroyed;
    }

    /**
     * Gets the average connection service time in milliseconds.
     *
     * @return the average service time (0 if no connections served)
     */
    public double getAverageConnectionServiceTimeMs() {
        long served = totalConnectionsServed.get();
        if (served == 0) {
            return 0;
        }
        return (double) totalActiveTimeMs.get() / served;
    }

    /**
     * Gets the average time to acquire a connection in milliseconds.
     *
     * @return the average acquisition time (0 if no connections requested)
     */
    public double getAverageAcquisitionTimeMs() {
        long requested = totalConnectionsRequested.get();
        if (requested == 0) {
            return 0;
        }
        return (double) (totalActiveTimeMs.get() + totalIdleTimeMs.get()) / requested;
    }

    /**
     * Gets the utilization percentage.
     *
     * @return the utilization percentage (0-100)
     */
    public double getUtilizationPercent() {
        int total = currentActiveConnections + currentIdleConnections;
        if (total == 0) {
            return 0;
        }
        return (double) currentActiveConnections / total * 100;
    }

    /**
     * Gets the hit rate percentage.
     *
     * @return the hit rate percentage (0-100)
     */
    public double getHitRatePercent() {
        long requested = totalConnectionsRequested.get();
        if (requested == 0) {
            return 100;
        }
        return (double) totalConnectionsServed.get() / requested * 100;
    }

    /**
     * Resets all statistics.
     */
    public void reset() {
        totalConnectionsCreated.set(0);
        totalConnectionsDestroyed.set(0);
        totalConnectionsRequested.set(0);
        totalConnectionsServed.set(0);
        totalConnectionTimeouts.set(0);
        totalValidationFailures.set(0);
        totalIdleTimeMs.set(0);
        totalActiveTimeMs.set(0);
        peakActiveConnections.set(0);
        peakIdleConnections.set(0);
        currentActiveConnections = 0;
        currentIdleConnections = 0;
        currentPendingConnections = 0;
    }

    /**
     * Gets a summary of the statistics.
     *
     * @return the summary
     */
    public String getSummary() {
        return String.format(
            "Pool: %s | Active: %d/%d | Idle: %d/%d | Pending: %d | Requests: %d | Served: %d | " +
            "Timeouts: %d | Utilization: %.2f%% | Hit Rate: %.2f%%",
            poolName,
            currentActiveConnections,
            peakActiveConnections.get(),
            currentIdleConnections,
            peakIdleConnections.get(),
            currentPendingConnections,
            totalConnectionsRequested.get(),
            totalConnectionsServed.get(),
            totalConnectionTimeouts.get(),
            getUtilizationPercent(),
            getHitRatePercent()
        );
    }
}
