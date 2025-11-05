package com.droid.bss.infrastructure.database.pooling;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Wrapper for a pooled database connection.
 *
 * Tracks connection state and provides lifecycle management.
 *
 * @since 1.0
 */
public class PooledConnection implements AutoCloseable {

    private final Connection connection;
    private final Instant createdAt;
    private final String poolName;
    private final AtomicBoolean inUse = new AtomicBoolean(false);
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private volatile Instant lastUsedAt;
    private volatile int useCount = 0;
    private volatile String lastQuery;

    /**
     * Creates a new PooledConnection.
     *
     * @param connection the underlying connection
     * @param poolName the name of the pool
     */
    public PooledConnection(Connection connection, String poolName) {
        this.connection = connection;
        this.poolName = poolName;
        this.createdAt = Instant.now();
        this.lastUsedAt = Instant.now();
    }

    /**
     * Gets the underlying connection.
     *
     * @return the connection
     * @throws SQLException if connection is closed
     */
    public Connection getConnection() throws SQLException {
        if (closed.get()) {
            throw new SQLException("Connection is closed");
        }
        return connection;
    }

    /**
     * Marks the connection as in use.
     *
     * @return true if successfully marked as in use
     */
    public boolean markInUse() {
        if (closed.get() || inUse.get()) {
            return false;
        }
        return inUse.compareAndSet(false, true);
    }

    /**
     * Marks the connection as released (not in use).
     */
    public void markReleased() {
        inUse.set(false);
        lastUsedAt = Instant.now();
    }

    /**
     * Checks if the connection is in use.
     *
     * @return true if in use
     */
    public boolean isInUse() {
        return inUse.get();
    }

    /**
     * Checks if the connection is closed.
     *
     * @return true if closed
     */
    public boolean isClosed() {
        return closed.get();
    }

    /**
     * Marks the connection as closed.
     */
    public void markClosed() {
        closed.set(true);
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
     * Gets the last used time.
     *
     * @return the last used time
     */
    public Instant getLastUsedAt() {
        return lastUsedAt;
    }

    /**
     * Gets the pool name.
     *
     * @return the pool name
     */
    public String getPoolName() {
        return poolName;
    }

    /**
     * Gets the number of times this connection has been used.
     *
     * @return the use count
     */
    public int getUseCount() {
        return useCount;
    }

    /**
     * Increments the use count.
     */
    public void incrementUseCount() {
        useCount++;
    }

    /**
     * Gets the last query executed on this connection.
     *
     * @return the last query (may be null)
     */
    public String getLastQuery() {
        return lastQuery;
    }

    /**
     * Sets the last query executed on this connection.
     *
     * @param lastQuery the last query
     */
    public void setLastQuery(String lastQuery) {
        this.lastQuery = lastQuery;
    }

    /**
     * Gets the age of the connection in milliseconds.
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
        return java.time.Duration.between(lastUsedAt, Instant.now()).toMillis();
    }

    /**
     * Validates the connection.
     *
     * @return true if valid
     * @throws SQLException if validation fails
     */
    public boolean validate() throws SQLException {
        if (closed.get()) {
            return false;
        }
        return connection != null && !connection.isClosed();
    }

    /**
     * Closes the connection (returns to pool).
     *
     * @throws SQLException if closing fails
     */
    @Override
    public void close() throws SQLException {
        if (closed.get()) {
            return;
        }

        try {
            if (inUse.get()) {
                // Connection is still in use, cannot close
                throw new SQLException("Cannot close a connection that is still in use");
            }

            // Mark as closed
            markClosed();

            // Close underlying connection
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }

        } finally {
            inUse.set(false);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PooledConnection that = (PooledConnection) o;
        return connection != null && connection.equals(that.connection);
    }

    @Override
    public int hashCode() {
        return connection != null ? connection.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "PooledConnection{" +
            "poolName='" + poolName + '\'' +
            ", inUse=" + inUse.get() +
            ", closed=" + closed.get() +
            ", useCount=" + useCount +
            ", ageMs=" + getAgeMs() +
            '}';
    }
}
