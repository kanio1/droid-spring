package com.droid.bss.infrastructure.database.pooling;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Configuration for connection pools.
 *
 * @since 1.0
 */
public class ConnectionPoolConfig {

    private final String poolName;
    private final String jdbcUrl;
    private final String username;
    private final String password;
    private final String driverClassName;

    // Pool sizing
    private final int minimumIdle;
    private final int maximumPoolSize;
    private final int initialPoolSize;

    // Timeouts
    private final Duration connectionTimeout;
    private final Duration idleTimeout;
    private final Duration maxLifetime;
    private final Duration leakDetectionThreshold;

    // Validation
    private final Duration validationTimeout;
    private final boolean testOnBorrow;
    private final boolean testOnReturn;
    private final boolean testWhileIdle;
    private final String validationQuery;

    // Connection settings
    private final boolean autoCommit;
    private final boolean readOnly;
    private final int transactionIsolation;
    private final boolean useSsl;

    // Performance
    private final boolean cachePrepStmts;
    private final int prepStmtCacheSize;
    private final int prepStmtCacheSqlLimit;
    private final boolean useServerPrepStmts;

    // Monitoring
    private final boolean jmxEnabled;
    private final boolean registerMbeans;
    private final Duration poolStatsMetricsInterval;

    private ConnectionPoolConfig(Builder builder) {
        this.poolName = builder.poolName;
        this.jdbcUrl = builder.jdbcUrl;
        this.username = builder.username;
        this.password = builder.password;
        this.driverClassName = builder.driverClassName;
        this.minimumIdle = builder.minimumIdle;
        this.maximumPoolSize = builder.maximumPoolSize;
        this.initialPoolSize = builder.initialPoolSize;
        this.connectionTimeout = builder.connectionTimeout;
        this.idleTimeout = builder.idleTimeout;
        this.maxLifetime = builder.maxLifetime;
        this.leakDetectionThreshold = builder.leakDetectionThreshold;
        this.validationTimeout = builder.validationTimeout;
        this.testOnBorrow = builder.testOnBorrow;
        this.testOnReturn = builder.testOnReturn;
        this.testWhileIdle = builder.testWhileIdle;
        this.validationQuery = builder.validationQuery;
        this.autoCommit = builder.autoCommit;
        this.readOnly = builder.readOnly;
        this.transactionIsolation = builder.transactionIsolation;
        this.useSsl = builder.useSsl;
        this.cachePrepStmts = builder.cachePrepStmts;
        this.prepStmtCacheSize = builder.prepStmtCacheSize;
        this.prepStmtCacheSqlLimit = builder.prepStmtCacheSqlLimit;
        this.useServerPrepStmts = builder.useServerPrepStmts;
        this.jmxEnabled = builder.jmxEnabled;
        this.registerMbeans = builder.registerMbeans;
        this.poolStatsMetricsInterval = builder.poolStatsMetricsInterval;
    }

    /**
     * Creates a builder for ConnectionPoolConfig.
     *
     * @param poolName the pool name
     * @param jdbcUrl the JDBC URL
     * @return the builder
     */
    public static Builder newBuilder(String poolName, String jdbcUrl) {
        return new Builder(poolName, jdbcUrl);
    }

    /**
     * Creates a builder with default values.
     *
     * @param poolName the pool name
     * @param jdbcUrl the JDBC URL
     * @param username the database username
     * @param password the database password
     * @return the builder
     */
    public static Builder newDefault(String poolName, String jdbcUrl, String username, String password) {
        return new Builder(poolName, jdbcUrl)
            .username(username)
            .password(password);
    }

    public String getPoolName() {
        return poolName;
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getDriverClassName() {
        return driverClassName;
    }

    public int getMinimumIdle() {
        return minimumIdle;
    }

    public int getMaximumPoolSize() {
        return maximumPoolSize;
    }

    public int getInitialPoolSize() {
        return initialPoolSize;
    }

    public Duration getConnectionTimeout() {
        return connectionTimeout;
    }

    public Duration getIdleTimeout() {
        return idleTimeout;
    }

    public Duration getMaxLifetime() {
        return maxLifetime;
    }

    public Duration getLeakDetectionThreshold() {
        return leakDetectionThreshold;
    }

    public Duration getValidationTimeout() {
        return validationTimeout;
    }

    public boolean isTestOnBorrow() {
        return testOnBorrow;
    }

    public boolean isTestOnReturn() {
        return testOnReturn;
    }

    public boolean isTestWhileIdle() {
        return testWhileIdle;
    }

    public String getValidationQuery() {
        return validationQuery;
    }

    public boolean isAutoCommit() {
        return autoCommit;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public int getTransactionIsolation() {
        return transactionIsolation;
    }

    public boolean isUseSsl() {
        return useSsl;
    }

    public boolean isCachePrepStmts() {
        return cachePrepStmts;
    }

    public int getPrepStmtCacheSize() {
        return prepStmtCacheSize;
    }

    public int getPrepStmtCacheSqlLimit() {
        return prepStmtCacheSqlLimit;
    }

    public boolean isUseServerPrepStmts() {
        return useServerPrepStmts;
    }

    public boolean isJmxEnabled() {
        return jmxEnabled;
    }

    public boolean isRegisterMbeans() {
        return registerMbeans;
    }

    public Duration getPoolStatsMetricsInterval() {
        return poolStatsMetricsInterval;
    }

    /**
     * Builder for ConnectionPoolConfig.
     */
    public static class Builder {
        private final String poolName;
        private final String jdbcUrl;

        private String username;
        private String password;
        private String driverClassName;

        private int minimumIdle = 5;
        private int maximumPoolSize = 20;
        private int initialPoolSize = 10;

        private Duration connectionTimeout = Duration.ofSeconds(30);
        private Duration idleTimeout = Duration.ofMinutes(10);
        private Duration maxLifetime = Duration.ofMinutes(30);
        private Duration leakDetectionThreshold = Duration.ofSeconds(60);

        private Duration validationTimeout = Duration.ofSeconds(5);
        private boolean testOnBorrow = true;
        private boolean testOnReturn = false;
        private boolean testWhileIdle = true;
        private String validationQuery = "SELECT 1";

        private boolean autoCommit = true;
        private boolean readOnly = false;
        private int transactionIsolation = -1; // Use default
        private boolean useSsl = false;

        private boolean cachePrepStmts = true;
        private int prepStmtCacheSize = 250;
        private int prepStmtCacheSqlLimit = 2048;
        private boolean useServerPrepStmts = true;

        private boolean jmxEnabled = true;
        private boolean registerMbeans = true;
        private Duration poolStatsMetricsInterval = Duration.ofSeconds(30);

        private Builder(String poolName, String jdbcUrl) {
            if (poolName == null || poolName.isBlank()) {
                throw new IllegalArgumentException("Pool name cannot be null or blank");
            }
            if (jdbcUrl == null || jdbcUrl.isBlank()) {
                throw new IllegalArgumentException("JDBC URL cannot be null or blank");
            }
            this.poolName = poolName;
            this.jdbcUrl = jdbcUrl;
        }

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public Builder password(String password) {
            this.password = password;
            return this;
        }

        public Builder driverClassName(String driverClassName) {
            this.driverClassName = driverClassName;
            return this;
        }

        public Builder minimumIdle(int minimumIdle) {
            if (minimumIdle < 0) {
                throw new IllegalArgumentException("Minimum idle cannot be negative");
            }
            this.minimumIdle = minimumIdle;
            return this;
        }

        public Builder maximumPoolSize(int maximumPoolSize) {
            if (maximumPoolSize <= 0) {
                throw new IllegalArgumentException("Maximum pool size must be positive");
            }
            if (maximumPoolSize < minimumIdle) {
                throw new IllegalArgumentException("Maximum pool size must be >= minimum idle");
            }
            this.maximumPoolSize = maximumPoolSize;
            return this;
        }

        public Builder initialPoolSize(int initialPoolSize) {
            if (initialPoolSize < 0) {
                throw new IllegalArgumentException("Initial pool size cannot be negative");
            }
            this.initialPoolSize = initialPoolSize;
            return this;
        }

        public Builder connectionTimeout(Duration connectionTimeout) {
            if (connectionTimeout == null || connectionTimeout.isNegative()) {
                throw new IllegalArgumentException("Connection timeout must be positive");
            }
            this.connectionTimeout = connectionTimeout;
            return this;
        }

        public Builder connectionTimeout(long timeout, TimeUnit unit) {
            return connectionTimeout(Duration.ofMillis(unit.toMillis(timeout)));
        }

        public Builder idleTimeout(Duration idleTimeout) {
            if (idleTimeout == null || idleTimeout.isNegative()) {
                throw new IllegalArgumentException("Idle timeout must be positive");
            }
            this.idleTimeout = idleTimeout;
            return this;
        }

        public Builder idleTimeout(long timeout, TimeUnit unit) {
            return idleTimeout(Duration.ofMillis(unit.toMillis(timeout)));
        }

        public Builder maxLifetime(Duration maxLifetime) {
            if (maxLifetime == null || maxLifetime.isNegative()) {
                throw new IllegalArgumentException("Max lifetime must be positive");
            }
            this.maxLifetime = maxLifetime;
            return this;
        }

        public Builder maxLifetime(long timeout, TimeUnit unit) {
            return maxLifetime(Duration.ofMillis(unit.toMillis(timeout)));
        }

        public Builder leakDetectionThreshold(Duration leakDetectionThreshold) {
            if (leakDetectionThreshold != null && leakDetectionThreshold.isNegative()) {
                throw new IllegalArgumentException("Leak detection threshold must be positive");
            }
            this.leakDetectionThreshold = leakDetectionThreshold;
            return this;
        }

        public Builder leakDetectionThreshold(long timeout, TimeUnit unit) {
            return leakDetectionThreshold(Duration.ofMillis(unit.toMillis(timeout)));
        }

        public Builder validationTimeout(Duration validationTimeout) {
            if (validationTimeout == null || validationTimeout.isNegative()) {
                throw new IllegalArgumentException("Validation timeout must be positive");
            }
            this.validationTimeout = validationTimeout;
            return this;
        }

        public Builder validationTimeout(long timeout, TimeUnit unit) {
            return validationTimeout(Duration.ofMillis(unit.toMillis(timeout)));
        }

        public Builder testOnBorrow(boolean testOnBorrow) {
            this.testOnBorrow = testOnBorrow;
            return this;
        }

        public Builder testOnReturn(boolean testOnReturn) {
            this.testOnReturn = testOnReturn;
            return this;
        }

        public Builder testWhileIdle(boolean testWhileIdle) {
            this.testWhileIdle = testWhileIdle;
            return this;
        }

        public Builder validationQuery(String validationQuery) {
            this.validationQuery = validationQuery;
            return this;
        }

        public Builder autoCommit(boolean autoCommit) {
            this.autoCommit = autoCommit;
            return this;
        }

        public Builder readOnly(boolean readOnly) {
            this.readOnly = readOnly;
            return this;
        }

        public Builder transactionIsolation(int transactionIsolation) {
            this.transactionIsolation = transactionIsolation;
            return this;
        }

        public Builder useSsl(boolean useSsl) {
            this.useSsl = useSsl;
            return this;
        }

        public Builder cachePrepStmts(boolean cachePrepStmts) {
            this.cachePrepStmts = cachePrepStmts;
            return this;
        }

        public Builder prepStmtCacheSize(int prepStmtCacheSize) {
            if (prepStmtCacheSize < 0) {
                throw new IllegalArgumentException("Prep stmt cache size cannot be negative");
            }
            this.prepStmtCacheSize = prepStmtCacheSize;
            return this;
        }

        public Builder prepStmtCacheSqlLimit(int prepStmtCacheSqlLimit) {
            if (prepStmtCacheSqlLimit < 0) {
                throw new IllegalArgumentException("Prep stmt cache SQL limit cannot be negative");
            }
            this.prepStmtCacheSqlLimit = prepStmtCacheSqlLimit;
            return this;
        }

        public Builder useServerPrepStmts(boolean useServerPrepStmts) {
            this.useServerPrepStmts = useServerPrepStmts;
            return this;
        }

        public Builder jmxEnabled(boolean jmxEnabled) {
            this.jmxEnabled = jmxEnabled;
            return this;
        }

        public Builder registerMbeans(boolean registerMbeans) {
            this.registerMbeans = registerMbeans;
            return this;
        }

        public Builder poolStatsMetricsInterval(Duration poolStatsMetricsInterval) {
            if (poolStatsMetricsInterval == null || poolStatsMetricsInterval.isNegative()) {
                throw new IllegalArgumentException("Pool stats metrics interval must be positive");
            }
            this.poolStatsMetricsInterval = poolStatsMetricsInterval;
            return this;
        }

        public Builder poolStatsMetricsInterval(long interval, TimeUnit unit) {
            return poolStatsMetricsInterval(Duration.ofMillis(unit.toMillis(interval)));
        }

        public ConnectionPoolConfig build() {
            return new ConnectionPoolConfig(this);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConnectionPoolConfig that = (ConnectionPoolConfig) o;
        return Objects.equals(poolName, that.poolName) &&
            Objects.equals(jdbcUrl, that.jdbcUrl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(poolName, jdbcUrl);
    }

    @Override
    public String toString() {
        return "ConnectionPoolConfig{" +
            "poolName='" + poolName + '\'' +
            ", jdbcUrl='" + jdbcUrl + '\'' +
            ", minimumIdle=" + minimumIdle +
            ", maximumPoolSize=" + maximumPoolSize +
            ", initialPoolSize=" + initialPoolSize +
            ", connectionTimeout=" + connectionTimeout +
            ", idleTimeout=" + idleTimeout +
            ", maxLifetime=" + maxLifetime +
            '}';
    }
}
