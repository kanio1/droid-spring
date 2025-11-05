package com.droid.bss.infrastructure.database.pooling;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration properties for connection pools.
 *
 * @since 1.0
 */
@Validated
@ConfigurationProperties(prefix = "app.db.pool")
public class ConnectionPoolProperties {

    /**
     * Whether connection pooling is enabled.
     */
    @NotNull
    private Boolean enabled = true;

    /**
     * The default minimum idle connections.
     */
    @Min(0)
    @Max(1000)
    private Integer minimumIdle = 5;

    /**
     * The default maximum pool size.
     */
    @Min(1)
    @Max(5000)
    private Integer maximumPoolSize = 20;

    /**
     * The default initial pool size.
     */
    @Min(0)
    @Max(1000)
    private Integer initialPoolSize = 10;

    /**
     * The connection timeout.
     */
    @NotNull
    private Duration connectionTimeout = Duration.ofSeconds(30);

    /**
     * The idle timeout.
     */
    @NotNull
    private Duration idleTimeout = Duration.ofMinutes(10);

    /**
     * The maximum lifetime of a connection.
     */
    @NotNull
    private Duration maxLifetime = Duration.ofMinutes(30);

    /**
     * The leak detection threshold.
     */
    private Duration leakDetectionThreshold = Duration.ofSeconds(60);

    /**
     * The validation timeout.
     */
    @NotNull
    private Duration validationTimeout = Duration.ofSeconds(5);

    /**
     * Whether to test connections on borrow.
     */
    @NotNull
    private Boolean testOnBorrow = true;

    /**
     * Whether to test connections on return.
     */
    @NotNull
    private Boolean testOnReturn = false;

    /**
     * Whether to test connections while idle.
     */
    @NotNull
    private Boolean testWhileIdle = true;

    /**
     * The validation query.
     */
    @NotBlank
    private String validationQuery = "SELECT 1";

    /**
     * Pool-specific configurations.
     */
    private Map<String, PoolConfig> pools = new HashMap<>();

    /**
     * Whether to enable JMX monitoring.
     */
    @NotNull
    private Boolean jmxEnabled = true;

    /**
     * Whether to register MBeans.
     */
    @NotNull
    private Boolean registerMbeans = true;

    /**
     * The metrics collection interval.
     */
    @NotNull
    private Duration metricsInterval = Duration.ofSeconds(30);

    public ConnectionPoolProperties() {}

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Integer getMinimumIdle() {
        return minimumIdle;
    }

    public void setMinimumIdle(Integer minimumIdle) {
        this.minimumIdle = minimumIdle;
    }

    public Integer getMaximumPoolSize() {
        return maximumPoolSize;
    }

    public void setMaximumPoolSize(Integer maximumPoolSize) {
        this.maximumPoolSize = maximumPoolSize;
    }

    public Integer getInitialPoolSize() {
        return initialPoolSize;
    }

    public void setInitialPoolSize(Integer initialPoolSize) {
        this.initialPoolSize = initialPoolSize;
    }

    public Duration getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(Duration connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public Duration getIdleTimeout() {
        return idleTimeout;
    }

    public void setIdleTimeout(Duration idleTimeout) {
        this.idleTimeout = idleTimeout;
    }

    public Duration getMaxLifetime() {
        return maxLifetime;
    }

    public void setMaxLifetime(Duration maxLifetime) {
        this.maxLifetime = maxLifetime;
    }

    public Duration getLeakDetectionThreshold() {
        return leakDetectionThreshold;
    }

    public void setLeakDetectionThreshold(Duration leakDetectionThreshold) {
        this.leakDetectionThreshold = leakDetectionThreshold;
    }

    public Duration getValidationTimeout() {
        return validationTimeout;
    }

    public void setValidationTimeout(Duration validationTimeout) {
        this.validationTimeout = validationTimeout;
    }

    public Boolean getTestOnBorrow() {
        return testOnBorrow;
    }

    public void setTestOnBorrow(Boolean testOnBorrow) {
        this.testOnBorrow = testOnBorrow;
    }

    public Boolean getTestOnReturn() {
        return testOnReturn;
    }

    public void setTestOnReturn(Boolean testOnReturn) {
        this.testOnReturn = testOnReturn;
    }

    public Boolean getTestWhileIdle() {
        return testWhileIdle;
    }

    public void setTestWhileIdle(Boolean testWhileIdle) {
        this.testWhileIdle = testWhileIdle;
    }

    public String getValidationQuery() {
        return validationQuery;
    }

    public void setValidationQuery(String validationQuery) {
        this.validationQuery = validationQuery;
    }

    public Map<String, PoolConfig> getPools() {
        return pools;
    }

    public void setPools(Map<String, PoolConfig> pools) {
        this.pools = pools != null ? pools : new HashMap<>();
    }

    public Boolean getJmxEnabled() {
        return jmxEnabled;
    }

    public void setJmxEnabled(Boolean jmxEnabled) {
        this.jmxEnabled = jmxEnabled;
    }

    public Boolean getRegisterMbeans() {
        return registerMbeans;
    }

    public void setRegisterMbeans(Boolean registerMbeans) {
        this.registerMbeans = registerMbeans;
    }

    public Duration getMetricsInterval() {
        return metricsInterval;
    }

    public void setMetricsInterval(Duration metricsInterval) {
        this.metricsInterval = metricsInterval;
    }

    /**
     * Gets pool configuration for a specific pool.
     *
     * @param poolName the pool name
     * @return the pool configuration (may be null)
     */
    public PoolConfig getPoolConfig(String poolName) {
        return pools.get(poolName);
    }

    /**
     * Adds a pool configuration.
     *
     * @param poolName the pool name
     * @param config the configuration
     */
    public void addPoolConfig(String poolName, PoolConfig config) {
        if (pools == null) {
            pools = new HashMap<>();
        }
        pools.put(poolName, config);
    }

    /**
     * Configuration for a specific pool.
     */
    public static class PoolConfig {

        /**
         * The JDBC URL.
         */
        @NotBlank
        private String jdbcUrl;

        /**
         * The database username.
         */
        private String username;

        /**
         * The database password.
         */
        private String password;

        /**
         * The JDBC driver class name.
         */
        private String driverClassName;

        /**
         * The minimum idle connections (default: inherit from global).
         */
        private Integer minimumIdle;

        /**
         * The maximum pool size (default: inherit from global).
         */
        private Integer maximumPoolSize;

        /**
         * The initial pool size (default: inherit from global).
         */
        private Integer initialPoolSize;

        /**
         * Additional connection properties.
         */
        private Map<String, String> properties = new HashMap<>();

        public PoolConfig() {}

        public PoolConfig(String jdbcUrl, String username, String password, String driverClassName) {
            this.jdbcUrl = jdbcUrl;
            this.username = username;
            this.password = password;
            this.driverClassName = driverClassName;
        }

        public String getJdbcUrl() {
            return jdbcUrl;
        }

        public void setJdbcUrl(String jdbcUrl) {
            this.jdbcUrl = jdbcUrl;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getDriverClassName() {
            return driverClassName;
        }

        public void setDriverClassName(String driverClassName) {
            this.driverClassName = driverClassName;
        }

        public Integer getMinimumIdle() {
            return minimumIdle;
        }

        public void setMinimumIdle(Integer minimumIdle) {
            this.minimumIdle = minimumIdle;
        }

        public Integer getMaximumPoolSize() {
            return maximumPoolSize;
        }

        public void setMaximumPoolSize(Integer maximumPoolSize) {
            this.maximumPoolSize = maximumPoolSize;
        }

        public Integer getInitialPoolSize() {
            return initialPoolSize;
        }

        public void setInitialPoolSize(Integer initialPoolSize) {
            this.initialPoolSize = initialPoolSize;
        }

        public Map<String, String> getProperties() {
            return properties;
        }

        public void setProperties(Map<String, String> properties) {
            this.properties = properties != null ? properties : new HashMap<>();
        }

        /**
         * Adds a property.
         *
         * @param key the property key
         * @param value the property value
         */
        public void addProperty(String key, String value) {
            if (properties == null) {
                properties = new HashMap<>();
            }
            properties.put(key, value);
        }

        /**
         * Gets a property value.
         *
         * @param key the property key
         * @return the property value (may be null)
         */
        public String getProperty(String key) {
            return properties.get(key);
        }

        /**
         * Gets a property value with default.
         *
         * @param key the property key
         * @param defaultValue the default value
         * @return the property value or default
         */
        public String getProperty(String key, String defaultValue) {
            return properties.getOrDefault(key, defaultValue);
        }
    }
}
