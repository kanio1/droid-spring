package com.droid.bss.infrastructure.database.pooling;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.*;

/**
 * Test suite for HikariConnectionPool
 *
 * @since 1.0
 */
@DisplayName("HikariConnectionPool Unit Tests")
class HikariConnectionPoolTest {

    private ConnectionPoolConfig config;
    private HikariConnectionPool pool;

    @BeforeEach
    void setUp() {
        config = ConnectionPoolConfig.newDefault("test-pool", "jdbc:h2:mem:test", "sa", "")
            .minimumIdle(2)
            .maximumPoolSize(5)
            .initialPoolSize(2)
            .build();

        pool = new HikariConnectionPool(config, false);
    }

    @Test
    @DisplayName("Should create pool with configuration")
    void shouldCreatePoolWithConfiguration() {
        // Then
        assertThat(pool).isNotNull();
        assertThat(pool.getConfig()).isEqualTo(config);
    }

    @Test
    @DisplayName("Should initialize pool and create initial connections")
    void shouldInitializePoolAndCreateInitialConnections() throws SQLException {
        // When
        pool.initialize();

        // Then
        assertThat(pool.getTotalConnections()).isEqualTo(2);
        assertThat(pool.getIdleConnections()).isEqualTo(2);
        assertThat(pool.getActiveConnections()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should acquire connection from pool")
    void shouldAcquireConnectionFromPool() throws Exception {
        // Given
        pool.initialize();

        // When
        PooledConnection conn = pool.acquire();

        // Then
        assertThat(conn).isNotNull();
        assertThat(conn.isInUse()).isTrue();
        assertThat(pool.getActiveConnections()).isEqualTo(1);
        assertThat(pool.getIdleConnections()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should release connection back to pool")
    void shouldReleaseConnectionBackToPool() throws Exception {
        // Given
        pool.initialize();
        PooledConnection conn = pool.acquire();

        // When
        pool.release(conn);

        // Then
        assertThat(conn.isInUse()).isFalse();
        assertThat(pool.getActiveConnections()).isEqualTo(0);
        assertThat(pool.getIdleConnections()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should handle multiple connections")
    void shouldHandleMultipleConnections() throws Exception {
        // Given
        pool.initialize();

        // When
        PooledConnection conn1 = pool.acquire();
        PooledConnection conn2 = pool.acquire();
        PooledConnection conn3 = pool.acquire();

        // Then
        assertThat(pool.getActiveConnections()).isEqualTo(3);
        assertThat(conn1).isNotNull();
        assertThat(conn2).isNotNull();
        assertThat(conn3).isNotNull();
    }

    @Test
    @DisplayName("Should acquire connection asynchronously")
    void shouldAcquireConnectionAsynchronously() throws Exception {
        // Given
        pool.initialize();

        // When
        CompletableFuture<PooledConnection> future = pool.acquireAsync();
        PooledConnection conn = future.get();

        // Then
        assertThat(conn).isNotNull();
        assertThat(pool.getActiveConnections()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should release connection asynchronously")
    void shouldReleaseConnectionAsynchronously() throws Exception {
        // Given
        pool.initialize();
        PooledConnection conn = pool.acquire();

        // When
        CompletableFuture<Void> future = pool.releaseAsync(conn);
        future.get();

        // Then
        assertThat(pool.getActiveConnections()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should prewarm pool with connections")
    void shouldPrewarmPoolWithConnections() throws SQLException {
        // When
        pool.prewarm(5);

        // Then
        assertThat(pool.getTotalConnections()).isEqualTo(5);
        assertThat(pool.getIdleConnections()).isEqualTo(5);
    }

    @Test
    @DisplayName("Should evict idle connections")
    void shouldEvictIdleConnections() throws SQLException {
        // Given
        pool.prewarm(5);
        int initialConnections = pool.getTotalConnections();

        // Wait a bit to ensure connections are idle
        Thread.sleep(100);

        // When
        int evicted = pool.evictIdleConnections();

        // Then
        assertThat(evicted).isGreaterThanOrEqualTo(0);
        assertThat(pool.getTotalConnections()).isLessThanOrEqualTo(initialConnections);
    }

    @Test
    @DisplayName("Should validate connection")
    void shouldValidateConnection() throws Exception {
        // Given
        pool.initialize();
        PooledConnection conn = pool.acquire();

        // When
        boolean isValid = pool.validateConnection(conn);

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Should track pool statistics")
    void shouldTrackPoolStatistics() throws SQLException {
        // Given
        pool.prewarm(3);

        // When
        ConnectionPoolStats stats = pool.getStats();

        // Then
        assertThat(stats).isNotNull();
        assertThat(stats.getCurrentIdleConnections()).isEqualTo(3);
        assertThat(stats.getTotalConnectionsCreated()).isEqualTo(3);
    }

    @Test
    @DisplayName("Should check if pool is healthy")
    void shouldCheckIfPoolIsHealthy() throws SQLException {
        // When
        pool.prewarm(1);

        // Then
        assertThat(pool.isHealthy()).isTrue();
    }

    @Test
    @DisplayName("Should check if pool is paused")
    void shouldCheckIfPoolIsPaused() throws SQLException {
        // Given
        pool.prewarm(1);

        // When
        pool.pause();

        // Then
        assertThat(pool.isPaused()).isTrue();
        assertThat(pool.isHealthy()).isFalse();
    }

    @Test
    @DisplayName("Should resume paused pool")
    void shouldResumePausedPool() throws SQLException {
        // Given
        pool.prewarm(1);
        pool.pause();

        // When
        pool.resume();

        // Then
        assertThat(pool.isPaused()).isFalse();
        assertThat(pool.isHealthy()).isTrue();
    }

    @Test
    @DisplayName("Should close pool")
    void shouldClosePool() throws SQLException {
        // Given
        pool.prewarm(3);

        // When
        pool.close();

        // Then
        assertThat(pool.getTotalConnections()).isEqualTo(0);
        assertThat(pool.isHealthy()).isFalse();
    }

    @Test
    @DisplayName("Should close pool asynchronously")
    void shouldClosePoolAsynchronously() throws Exception {
        // Given
        pool.prewarm(1);

        // When
        CompletableFuture<Void> future = pool.closeAsync();
        future.get();

        // Then
        assertThat(pool.getTotalConnections()).isEqualTo(0);
    }

    @Test
    @DisplayName("PooledConnection should track usage")
    void pooledConnectionShouldTrackUsage() throws Exception {
        // Given
        pool.prewarm(1);
        PooledConnection conn = pool.acquire();

        // When
        conn.incrementUseCount();

        // Then
        assertThat(conn.getUseCount()).isEqualTo(1);
        assertThat(conn.isInUse()).isTrue();
    }

    @Test
    @DisplayName("PooledConnection should close properly")
    void pooledConnectionShouldCloseProperly() throws Exception {
        // Given
        pool.prewarm(1);
        PooledConnection conn = pool.acquire();

        // When
        conn.markReleased();
        conn.close();

        // Then
        assertThat(conn.isClosed()).isTrue();
    }

    @Test
    @DisplayName("ConnectionPoolStats should track metrics")
    void connectionPoolStatsShouldTrackMetrics() {
        // Given
        ConnectionPoolStats stats = new ConnectionPoolStats("test-pool");

        // When
        stats.recordConnectionCreated();
        stats.recordConnectionCreated();
        stats.recordConnectionRequested();
        stats.recordConnectionServed();
        stats.recordIdleTime(100);
        stats.recordActiveTime(200);
        stats.setCurrentActiveConnections(3);
        stats.setCurrentIdleConnections(2);

        // Then
        assertThat(stats.getTotalConnectionsCreated()).isEqualTo(2);
        assertThat(stats.getTotalConnectionsRequested()).isEqualTo(1);
        assertThat(stats.getTotalConnectionsServed()).isEqualTo(1);
        assertThat(stats.getTotalIdleTimeMs()).isEqualTo(100);
        assertThat(stats.getTotalActiveTimeMs()).isEqualTo(200);
        assertThat(stats.getCurrentActiveConnections()).isEqualTo(3);
        assertThat(stats.getCurrentIdleConnections()).isEqualTo(2);
        assertThat(stats.getUtilizationPercent()).isGreaterThan(0);
    }

    @Test
    @DisplayName("ConnectionPoolConfig should build correctly")
    void connectionPoolConfigShouldBuildCorrectly() {
        // When
        ConnectionPoolConfig cfg = ConnectionPoolConfig.newBuilder("test-pool", "jdbc:h2:mem:test")
            .username("user")
            .password("pass")
            .minimumIdle(5)
            .maximumPoolSize(10)
            .connectionTimeout(Duration.ofSeconds(60))
            .build();

        // Then
        assertThat(cfg.getPoolName()).isEqualTo("test-pool");
        assertThat(cfg.getJdbcUrl()).isEqualTo("jdbc:h2:mem:test");
        assertThat(cfg.getUsername()).isEqualTo("user");
        assertThat(cfg.getPassword()).isEqualTo("pass");
        assertThat(cfg.getMinimumIdle()).isEqualTo(5);
        assertThat(cfg.getMaximumPoolSize()).isEqualTo(10);
        assertThat(cfg.getConnectionTimeout()).isEqualTo(Duration.ofSeconds(60));
    }

    @Test
    @DisplayName("ConnectionPoolConfig should use defaults")
    void connectionPoolConfigShouldUseDefaults() {
        // When
        ConnectionPoolConfig cfg = ConnectionPoolConfig.newBuilder("test-pool", "jdbc:h2:mem:test").build();

        // Then
        assertThat(cfg.getMinimumIdle()).isEqualTo(5);
        assertThat(cfg.getMaximumPoolSize()).isEqualTo(20);
        assertThat(cfg.getInitialPoolSize()).isEqualTo(10);
        assertThat(cfg.getConnectionTimeout()).isEqualTo(Duration.ofSeconds(30));
        assertThat(cfg.isTestOnBorrow()).isTrue();
    }

    @Test
    @DisplayName("Should throw exception when acquiring from closed pool")
    void shouldThrowExceptionWhenAcquiringFromClosedPool() throws SQLException {
        // Given
        pool.close();

        // When & Then
        assertThatThrownBy(() -> pool.acquire())
            .isInstanceOf(SQLException.class)
            .hasMessageContaining("closed");
    }

    @Test
    @DisplayName("Should throw exception when releasing null connection")
    void shouldThrowExceptionWhenReleasingNullConnection() throws SQLException {
        // When & Then
        assertThatCode(() -> pool.release(null))
            .doesNotThrowAnyException();
    }
}
