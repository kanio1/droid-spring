package com.droid.bss.infrastructure.event.handlers;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration properties for event handlers.
 *
 * @since 1.0
 */
@Validated
@ConfigurationProperties(prefix = "app.event.handler")
public class EventHandlerProperties {

    /**
     * Whether event handling is enabled.
     */
    @NotNull
    private Boolean enabled = true;

    /**
     * Whether to automatically acknowledge Kafka messages.
     */
    @NotNull
    private Boolean autoAcknowledge = true;

    /**
     * The default concurrency level for handler execution.
     */
    @Min(1)
    @Max(100)
    private Integer concurrency = 10;

    /**
     * The batch size for batch processing.
     */
    @Min(1)
    @Max(10000)
    private Integer batchSize = 100;

    /**
     * The maximum number of retry attempts.
     */
    @Min(0)
    @Max(10)
    private Integer maxRetries = 3;

    /**
     * The retry delay in milliseconds.
     */
    @Min(0)
    @Max(60000)
    private Long retryDelayMs = 1000L;

    /**
     * Whether to enable handler statistics.
     */
    @NotNull
    private Boolean statisticsEnabled = true;

    /**
     * Handler-specific configurations.
     */
    private Map<String, HandlerConfig> handlers = new HashMap<>();

    /**
     * The timeout for handler execution in milliseconds.
     */
    @Min(1000)
    @Max(300000)
    private Long executionTimeoutMs = 30000L;

    public EventHandlerProperties() {}

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Boolean getAutoAcknowledge() {
        return autoAcknowledge;
    }

    public void setAutoAcknowledge(Boolean autoAcknowledge) {
        this.autoAcknowledge = autoAcknowledge;
    }

    public Integer getConcurrency() {
        return concurrency;
    }

    public void setConcurrency(Integer concurrency) {
        this.concurrency = concurrency;
    }

    public Integer getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(Integer batchSize) {
        this.batchSize = batchSize;
    }

    public Integer getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(Integer maxRetries) {
        this.maxRetries = maxRetries;
    }

    public Long getRetryDelayMs() {
        return retryDelayMs;
    }

    public void setRetryDelayMs(Long retryDelayMs) {
        this.retryDelayMs = retryDelayMs;
    }

    public Boolean getStatisticsEnabled() {
        return statisticsEnabled;
    }

    public void setStatisticsEnabled(Boolean statisticsEnabled) {
        this.statisticsEnabled = statisticsEnabled;
    }

    public Map<String, HandlerConfig> getHandlers() {
        return handlers;
    }

    public void setHandlers(Map<String, HandlerConfig> handlers) {
        this.handlers = handlers != null ? handlers : new HashMap<>();
    }

    public Long getExecutionTimeoutMs() {
        return executionTimeoutMs;
    }

    public void setExecutionTimeoutMs(Long executionTimeoutMs) {
        this.executionTimeoutMs = executionTimeoutMs;
    }

    /**
     * Gets handler configuration for a specific handler.
     *
     * @param handlerName the handler name
     * @return the handler configuration (may be null)
     */
    public HandlerConfig getHandlerConfig(String handlerName) {
        return handlers.get(handlerName);
    }

    /**
     * Adds a handler configuration.
     *
     * @param handlerName the handler name
     * @param config the configuration
     */
    public void addHandlerConfig(String handlerName, HandlerConfig config) {
        if (handlers == null) {
            handlers = new HashMap<>();
        }
        handlers.put(handlerName, config);
    }

    /**
     * Configuration for a specific handler.
     */
    public static class HandlerConfig {

        /**
         * Whether this handler is enabled.
         */
        @NotNull
        private Boolean enabled = true;

        /**
         * The priority of this handler.
         */
        @Min(-1000)
        @Max(1000)
        private Integer priority = 0;

        /**
         * The number of threads for this handler.
         */
        @Min(1)
        @Max(100)
        private Integer threads = 1;

        /**
         * Whether to enable logging for this handler.
         */
        @NotNull
        private Boolean loggingEnabled = true;

        /**
         * Additional properties for the handler.
         */
        private Map<String, String> properties = new HashMap<>();

        public HandlerConfig() {}

        public HandlerConfig(Boolean enabled, Integer priority, Integer threads, Boolean loggingEnabled) {
            this.enabled = enabled;
            this.priority = priority;
            this.threads = threads;
            this.loggingEnabled = loggingEnabled;
        }

        public Boolean getEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }

        public Integer getPriority() {
            return priority;
        }

        public void setPriority(Integer priority) {
            this.priority = priority;
        }

        public Integer getThreads() {
            return threads;
        }

        public void setThreads(Integer threads) {
            this.threads = threads;
        }

        public Boolean getLoggingEnabled() {
            return loggingEnabled;
        }

        public void setLoggingEnabled(Boolean loggingEnabled) {
            this.loggingEnabled = loggingEnabled;
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
