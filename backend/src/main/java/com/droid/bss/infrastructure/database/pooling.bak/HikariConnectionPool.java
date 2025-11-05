package com.droid.bss.infrastructure.database.pooling;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.Lock;

/**
 * HikariCP-based implementation of ConnectionPool.
 *
 * @since 1.0
 */
public class HikariConnectionPool implements ConnectionPool {

    private static final Logger log = LoggerFactory.getLogger(HikariConnectionPool.class);

    private final ConnectionPoolConfig config;
    private final ConnectionPoolStats stats;
    private final HikariDataSource dataSource;

    private final BlockingQueue<PooledConnection> availableConnections = new LinkedBlockingQueue<>();
    private final ConcurrentHashMap<Integer, PooledConnection> activeConnections = new ConcurrentHashMap<>();
    private final AtomicInteger connectionIdGenerator = new AtomicInteger(0);
    private final AtomicLong pendingConnections = new AtomicLong(0);

    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final Lock readLock = rwLock.readLock();
    private final Lock writeLock = rwLock.writeLock();

    private volatile boolean initialized = false;
    private volatile boolean closed = false;
    private volatile boolean paused = false;
    private volatile ScheduledExecutorService scheduler;

    /**
     * Creates a new HikariConnectionPool.
     *
     * @param config the configuration
     * @throws SQLException if initialization fails
     */
    public HikariConnectionPool(ConnectionPoolConfig config) throws SQLException {
        this.config = config;
        this.stats = new ConnectionPoolStats(config.getPoolName());
        this.dataSource = createHikariDataSource(config);
        initialize();
    }

    /**
     * Creates a new HikariConnectionPool without initializing.
     *
     * @param config the configuration
     */
    public HikariConnectionPool(ConnectionPoolConfig config, boolean initialize) {
        this.config = config;
        this.stats = new ConnectionPoolStats(config.getPoolName());
        this.dataSource = createHikariDataSource(config);
        if (initialize) {
            try {
                initialize();
            } catch (SQLException e) {
                throw new RuntimeException("Failed to initialize connection pool", e);
            }
        }
    }

    @Override
    public PooledConnection acquire() throws SQLException, InterruptedException {
        if (closed) {
            throw new SQLException("Connection pool is closed");
        }

        if (paused) {
            throw new SQLException("Connection pool is paused");
        }

        stats.recordConnectionRequested();
        pendingConnections.incrementAndGet();

        try {
            readLock.lock();
            if (paused || closed) {
                pendingConnections.decrementAndGet();
                throw new SQLException("Connection pool is paused or closed");
            }

            // Try to get connection from available pool
            PooledConnection conn = availableConnections.poll();

            if (conn != null && conn.validate()) {
                return useConnection(conn);
            }

            // Create new connection if pool allows
            if (getTotalConnections() < config.getMaximumPoolSize()) {
                conn = createConnection();
                return useConnection(conn);
            }

            // Wait for available connection
            readLock.unlock();

            try {
                long timeoutMs = config.getConnectionTimeout().toMillis();
                long startTime = System.currentTimeMillis();

                while (System.currentTimeMillis() - startTime < timeoutMs) {
                    readLock.lock();
                    conn = availableConnections.poll();

                    if (conn != null && conn.validate()) {
                        return useConnection(conn);
                    }

                    readLock.unlock();

                    // Wait a bit before retrying
                    Thread.sleep(100);
                }

                stats.recordConnectionTimeout();
                throw new SQLException("Timeout waiting for connection from pool");
            } finally {
                pendingConnections.decrementAndGet();
            }

        } finally {
            if (readLock.tryLock()) {
                readLock.unlock();
            }
        }
    }

    @Override
    public CompletableFuture<PooledConnection> acquireAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return acquire();
            } catch (SQLException | InterruptedException e) {
                throw new CompletionException(e);
            }
        });
    }

    @Override
    public void release(PooledConnection connection) throws SQLException {
        if (connection == null) {
            return;
        }

        try {
            if (connection.isInUse()) {
                connection.markReleased();

                if (connection.validate()) {
                    availableConnections.offer(connection);
                } else {
                    destroyConnection(connection);
                }

                updateStats();
            }
        } catch (SQLException e) {
            // Connection is invalid, destroy it
            destroyConnection(connection);
            throw e;
        }
    }

    @Override
    public CompletableFuture<Void> releaseAsync(PooledConnection connection) {
        return CompletableFuture.runAsync(() -> {
            try {
                release(connection);
            } catch (SQLException e) {
                throw new CompletionException(e);
            }
        });
    }

    @Override
    public int getTotalConnections() {
        return availableConnections.size() + activeConnections.size();
    }

    @Override
    public int getIdleConnections() {
        return availableConnections.size();
    }

    @Override
    public int getActiveConnections() {
        return activeConnections.size();
    }

    @Override
    public int getPendingConnections() {
        return (int) pendingConnections.get();
    }

    @Override
    public ConnectionPoolStats getStats() {
        return stats;
    }

    @Override
    public boolean isHealthy() {
        try {
            return !closed && !paused && dataSource.isRunning() && getTotalConnections() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void initialize() throws SQLException {
        if (initialized) {
            return;
        }

        writeLock.lock();
        try {
            if (initialized) {
                return;
            }

            // Pre-warm pool
            prewarm(config.getInitialPoolSize());

            // Start metrics scheduler
            startMetricsScheduler();

            initialized = true;
            log.info("Connection pool '{}' initialized with {} connections",
                config.getPoolName(), config.getInitialPoolSize());

        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void close() throws SQLException {
        if (closed) {
            return;
        }

        writeLock.lock();
        try {
            if (closed) {
                return;
            }

            closed = true;
            paused = false;

            // Stop scheduler
            if (scheduler != null) {
                scheduler.shutdown();
                try {
                    scheduler.awaitTermination(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            // Close all connections
            closeAllConnections();

            // Close data source
            if (dataSource != null && !dataSource.isClosed()) {
                dataSource.close();
            }

            log.info("Connection pool '{}' closed", config.getPoolName());

        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public CompletableFuture<Void> closeAsync() {
        return CompletableFuture.runAsync(() -> {
            try {
                close();
            } catch (SQLException e) {
                throw new CompletionException(e);
            }
        });
    }

    @Override
    public boolean validateConnection(PooledConnection connection) throws SQLException {
        if (connection == null) {
            return false;
        }
        return connection.validate();
    }

    @Override
    public void prewarm(int count) throws SQLException {
        writeLock.lock();
        try {
            for (int i = 0; i < count; i++) {
                PooledConnection conn = createConnection();
                availableConnections.offer(conn);
                stats.recordConnectionCreated();
            }
            updateStats();
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public int evictIdleConnections() throws SQLException {
        writeLock.lock();
        try {
            int evicted = 0;
            int targetIdle = Math.min(config.getMinimumIdle(), availableConnections.size());

            PooledConnection[] connections = availableConnections.toArray(new PooledConnection[0]);

            for (PooledConnection conn : connections) {
                if (availableConnections.size() <= targetIdle) {
                    break;
                }

                // Check if connection is idle for too long
                long idleTimeMs = conn.getIdleTimeMs();
                long idleThreshold = config.getIdleTimeout().toMillis();

                if (idleTimeMs > idleThreshold) {
                    if (availableConnections.remove(conn)) {
                        destroyConnection(conn);
                        evicted++;
                    }
                }
            }

            if (evicted > 0) {
                log.debug("Evicted {} idle connections from pool '{}'", evicted, config.getPoolName());
            }

            return evicted;

        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public ConnectionPoolConfig getConfig() {
        return config;
    }

    @Override
    public void pause() {
        writeLock.lock();
        try {
            paused = true;
            log.info("Connection pool '{}' paused", config.getPoolName());
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void resume() {
        writeLock.lock();
        try {
            paused = false;
            log.info("Connection pool '{}' resumed", config.getPoolName());
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public boolean isPaused() {
        return paused;
    }

    // Private helper methods

    private HikariDataSource createHikariDataSource(ConnectionPoolConfig config) {
        HikariConfig hikariConfig = new HikariConfig();

        hikariConfig.setJdbcUrl(config.getJdbcUrl());
        hikariConfig.setUsername(config.getUsername());
        hikariConfig.setPassword(config.getPassword());
        hikariConfig.setDriverClassName(config.getDriverClassName());

        hikariConfig.setMinimumIdle(config.getMinimumIdle());
        hikariConfig.setMaximumPoolSize(config.getMaximumPoolSize());
        hikariConfig.setConnectionTimeout(config.getConnectionTimeout().toMillis());
        hikariConfig.setIdleTimeout(config.getIdleTimeout().toMillis());
        hikariConfig.setMaxLifetime(config.getMaxLifetime().toMillis());
        hikariConfig.setLeakDetectionThreshold(config.getLeakDetectionThreshold().toMillis());

        hikariConfig.setValidationTimeout(config.getValidationTimeout().toMillis());
        // Note: Test methods removed in newer HikariCP versions
        // hikariConfig.setTestOnBorrow(config.isTestOnBorrow());
        // hikariConfig.setTestOnReturn(config.isTestOnReturn());
        // hikariConfig.setTestWhileIdle(config.isTestWhileIdle());
        hikariConfig.setConnectionInitSql(config.getValidationQuery());

        hikariConfig.setAutoCommit(config.isAutoCommit());
        hikariConfig.setReadOnly(config.isReadOnly());
        hikariConfig.setTransactionIsolation(config.getTransactionIsolation());
        // Note: SSL method removed/changed in newer HikariCP versions
        // hikariConfig.setUseSSL(config.isUseSsl());

        // Note: Prep statement cache methods removed in newer HikariCP versions
        // hikariConfig.setCachePrepStmts(config.isCachePrepStmts());
        // hikariConfig.setPrepStmtCacheSize(config.getPrepStmtCacheSize());
        // hikariConfig.setPrepStmtCacheSqlLimit(config.getPrepStmtCacheSqlLimit());
        // hikariConfig.setUseServerPrepStmts(config.isUseServerPrepStmts());

        // Note: JMX methods removed in newer HikariCP versions
        // hikariConfig.setJmxName("com.droid.bss.pool." + config.getPoolName());
        hikariConfig.setRegisterMbeans(config.isRegisterMbeans());

        return new HikariDataSource(hikariConfig);
    }

    private PooledConnection createConnection() throws SQLException {
        if (closed) {
            throw new SQLException("Connection pool is closed");
        }

        Connection conn = dataSource.getConnection();
        int id = connectionIdGenerator.incrementAndGet();

        PooledConnection pooled = new PooledConnection(conn, config.getPoolName());

        log.trace("Created new connection: pool={}, id={}", config.getPoolName(), id);
        return pooled;
    }

    private PooledConnection useConnection(PooledConnection conn) {
        int id = connectionIdGenerator.incrementAndGet();

        conn.incrementUseCount();
        conn.markInUse();

        activeConnections.put(id, conn);
        stats.recordConnectionServed();

        updateStats();

        log.trace("Acquired connection: pool={}, id={}", config.getPoolName(), id);
        return conn;
    }

    private void destroyConnection(PooledConnection conn) {
        try {
            if (conn != null && !conn.isClosed()) {
                conn.close();
                stats.recordConnectionDestroyed();
            }
        } catch (SQLException e) {
            log.error("Error closing connection in pool '{}': {}", config.getPoolName(), e.getMessage());
        }

        updateStats();
    }

    private void closeAllConnections() {
        // Close available connections
        availableConnections.forEach(this::destroyConnection);
        availableConnections.clear();

        // Close active connections
        activeConnections.values().forEach(this::destroyConnection);
        activeConnections.clear();

        updateStats();
    }

    private void updateStats() {
        stats.setCurrentActiveConnections(getActiveConnections());
        stats.setCurrentIdleConnections(getIdleConnections());
        stats.setCurrentPendingConnections(getPendingConnections());
    }

    private void startMetricsScheduler() {
        scheduler = Executors.newScheduledThreadPool(1, r -> {
            Thread t = new Thread(r, "pool-metrics-" + config.getPoolName());
            t.setDaemon(true);
            return t;
        });

        scheduler.scheduleAtFixedRate(() -> {
            try {
                updateStats();
            } catch (Exception e) {
                log.error("Error updating pool metrics: {}", e.getMessage());
            }
        }, 0, config.getPoolStatsMetricsInterval().toMillis(), TimeUnit.MILLISECONDS);
    }
}
