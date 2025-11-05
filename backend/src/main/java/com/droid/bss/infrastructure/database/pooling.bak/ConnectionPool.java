package com.droid.bss.infrastructure.database.pooling;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;

/**
 * Generic interface for connection pooling.
 *
 * Provides lifecycle management for database connections.
 *
 * @since 1.0
 */
public interface ConnectionPool {

    /**
     * Acquires a connection from the pool.
     *
     * @return the pooled connection
     * @throws SQLException if acquiring connection fails
     * @throws InterruptedException if waiting is interrupted
     */
    PooledConnection acquire() throws SQLException, InterruptedException;

    /**
     * Acquires a connection asynchronously from the pool.
     *
     * @return the CompletableFuture with pooled connection
     */
    CompletableFuture<PooledConnection> acquireAsync();

    /**
     * Releases a connection back to the pool.
     *
     * @param connection the connection to release
     * @throws SQLException if releasing fails
     */
    void release(PooledConnection connection) throws SQLException;

    /**
     * Releases a connection asynchronously back to the pool.
     *
     * @param connection the connection to release
     * @return the CompletableFuture
     */
    CompletableFuture<Void> releaseAsync(PooledConnection connection);

    /**
     * Gets the total number of connections in the pool.
     *
     * @return the total connections
     */
    int getTotalConnections();

    /**
     * Gets the number of idle connections.
     *
     * @return the idle connections
     */
    int getIdleConnections();

    /**
     * Gets the number of active (in-use) connections.
     *
     * @return the active connections
     */
    int getActiveConnections();

    /**
     * Gets the number of connections pending acquisition.
     *
     * @return the pending connections
     */
    int getPendingConnections();

    /**
     * Gets the pool statistics.
     *
     * @return the statistics
     */
    ConnectionPoolStats getStats();

    /**
     * Checks if the pool is healthy.
     *
     * @return true if healthy
     */
    boolean isHealthy();

    /**
     * Initializes the connection pool.
     *
     * @throws SQLException if initialization fails
     */
    void initialize() throws SQLException;

    /**
     * Closes the connection pool.
     *
     * @throws SQLException if closing fails
     */
    void close() throws SQLException;

    /**
     * Closes the connection pool asynchronously.
     *
     * @return the CompletableFuture
     */
    CompletableFuture<Void> closeAsync();

    /**
     * Validates a connection.
     *
     * @param connection the connection to validate
     * @return true if valid
     * @throws SQLException if validation fails
     */
    boolean validateConnection(PooledConnection connection) throws SQLException;

    /**
     * Pre-warms the pool by creating initial connections.
     *
     * @param count the number of connections to create
     * @throws SQLException if pre-warming fails
     */
    void prewarm(int count) throws SQLException;

    /**
     * Evicts idle connections.
     *
     * @return the number of connections evicted
     * @throws SQLException if eviction fails
     */
    int evictIdleConnections() throws SQLException;

    /**
     * Gets the pool configuration.
     *
     * @return the configuration
     */
    ConnectionPoolConfig getConfig();

    /**
     * Pauses the pool (no new connections can be acquired).
     */
    void pause();

    /**
     * Resumes the pool (new connections can be acquired).
     */
    void resume();

    /**
     * Checks if the pool is paused.
     *
     * @return true if paused
     */
    boolean isPaused();
}
