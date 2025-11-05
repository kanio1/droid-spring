package com.droid.bss.infrastructure.cache.eviction;

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
 * Configuration properties for cache eviction.
 *
 * @since 1.0
 */
@Validated
@ConfigurationProperties(prefix = "app.cache.eviction")
public class CacheEvictionProperties {

    /**
     * Whether cache eviction is enabled.
     */
    @NotNull
    private Boolean enabled = true;

    /**
     * The default eviction policy.
     */
    @NotBlank
    private String defaultPolicy = "LRU";

    /**
     * The default maximum cache size (number of entries).
     */
    @Min(1)
    @Max(1000000)
    private Integer maxCacheSize = 1000;

    /**
     * The default time-to-live in seconds.
     */
    @Min(0)
    @Max(86400)
    private Integer defaultTtlSeconds = 3600; // 1 hour

    /**
     * Whether to enable automatic cleanup of expired entries.
     */
    @NotNull
    private Boolean autoCleanupEnabled = true;

    /**
     * The cleanup interval in seconds.
     */
    @Min(1)
    @Max(3600)
    private Integer cleanupIntervalSeconds = 60;

    /**
     * Whether to track eviction statistics.
     */
    @NotNull
    private Boolean statisticsEnabled = true;

    /**
     * Cache-specific configurations.
     */
    private Map<String, CacheEvictionConfig> caches = new HashMap<>();

    /**
     * The default idle threshold in milliseconds.
     */
    @Min(0)
    @Max(86400000)
    private Long idleThresholdMs = 0L; // No idle eviction by default

    /**
     * Whether to enable logging.
     */
    @NotNull
    private Boolean loggingEnabled = false;

    /**
     * The log level for eviction operations.
     */
    @NotNull
    private String logLevel = "DEBUG";

    public CacheEvictionProperties() {}

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getDefaultPolicy() {
        return defaultPolicy;
    }

    public void setDefaultPolicy(String defaultPolicy) {
        this.defaultPolicy = defaultPolicy;
    }

    public Integer getMaxCacheSize() {
        return maxCacheSize;
    }

    public void setMaxCacheSize(Integer maxCacheSize) {
        this.maxCacheSize = maxCacheSize;
    }

    public Integer getDefaultTtlSeconds() {
        return defaultTtlSeconds;
    }

    public void setDefaultTtlSeconds(Integer defaultTtlSeconds) {
        this.defaultTtlSeconds = defaultTtlSeconds;
    }

    public Boolean getAutoCleanupEnabled() {
        return autoCleanupEnabled;
    }

    public void setAutoCleanupEnabled(Boolean autoCleanupEnabled) {
        this.autoCleanupEnabled = autoCleanupEnabled;
    }

    public Integer getCleanupIntervalSeconds() {
        return cleanupIntervalSeconds;
    }

    public void setCleanupIntervalSeconds(Integer cleanupIntervalSeconds) {
        this.cleanupIntervalSeconds = cleanupIntervalSeconds;
    }

    public Boolean getStatisticsEnabled() {
        return statisticsEnabled;
    }

    public void setStatisticsEnabled(Boolean statisticsEnabled) {
        this.statisticsEnabled = statisticsEnabled;
    }

    public Map<String, CacheEvictionConfig> getCaches() {
        return caches;
    }

    public void setCaches(Map<String, CacheEvictionConfig> caches) {
        this.caches = caches != null ? caches : new HashMap<>();
    }

    public Long getIdleThresholdMs() {
        return idleThresholdMs;
    }

    public void setIdleThresholdMs(Long idleThresholdMs) {
        this.idleThresholdMs = idleThresholdMs;
    }

    public Boolean getLoggingEnabled() {
        return loggingEnabled;
    }

    public void setLoggingEnabled(Boolean loggingEnabled) {
        this.loggingEnabled = loggingEnabled;
    }

    public String getLogLevel() {
        return logLevel;
    }

    public void setLogLevel(String logLevel) {
        this.logLevel = logLevel;
    }

    /**
     * Gets cache configuration for a specific cache.
     *
     * @param cacheName the cache name
     * @return the cache configuration (may be null)
     */
    public CacheEvictionConfig getCacheConfig(String cacheName) {
        return caches.get(cacheName);
    }

    /**
     * Adds a cache configuration.
     *
     * @param cacheName the cache name
     * @param config the configuration
     */
    public void addCacheConfig(String cacheName, CacheEvictionConfig config) {
        if (caches == null) {
            caches = new HashMap<>();
        }
        caches.put(cacheName, config);
    }

    /**
     * Configuration for a specific cache.
     */
    public static class CacheEvictionConfig {

        /**
         * The eviction policy for this cache.
         */
        private String policy = "LRU";

        /**
         * The maximum cache size (number of entries).
         */
        private Integer maxCacheSize;

        /**
         * The time-to-live in milliseconds.
         */
        private Long ttlMs;

        /**
         * The idle threshold in milliseconds.
         */
        private Long idleThresholdMs;

        /**
         * Additional properties for the policy.
         */
        private Map<String, String> properties = new HashMap<>();

        public CacheEvictionConfig() {}

        public String getPolicy() {
            return policy;
        }

        public void setPolicy(String policy) {
            this.policy = policy;
        }

        public Integer getMaxCacheSize() {
            return maxCacheSize;
        }

        public void setMaxCacheSize(Integer maxCacheSize) {
            this.maxCacheSize = maxCacheSize;
        }

        public Long getTtlMs() {
            return ttlMs;
        }

        public void setTtlMs(Long ttlMs) {
            this.ttlMs = ttlMs;
        }

        public Long getIdleThresholdMs() {
            return idleThresholdMs;
        }

        public void setIdleThresholdMs(Long idleThresholdMs) {
            this.idleThresholdMs = idleThresholdMs;
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
