package com.droid.bss.infrastructure.messaging.deadletter;

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
 * Configuration for Dead Letter Queue.
 *
 * @since 1.0
 */
@Validated
@ConfigurationProperties(prefix = "app.dlq")
public class DeadLetterQueueConfig {

    /**
     * Whether DLQ is enabled.
     */
    @NotNull
    private Boolean enabled = true;

    /**
     * The DLQ name.
     */
    @NotBlank
    private String name = "default-dlq";

    /**
     * The DLQ topic prefix.
     */
    @NotBlank
    private String topicPrefix = "dlq";

    /**
     * The maximum number of messages to keep in DLQ.
     */
    @Min(100)
    @Max(10000000)
    private Long maxMessages = 100000L;

    /**
     * The retention time for DLQ messages.
     */
    @NotNull
    private Duration retentionTime = Duration.ofDays(7);

    /**
     * The maximum payload size in bytes.
     */
    @Min(1024)
    @Max(10485760)
    private Long maxPayloadSize = 1024000L; // 1MB

    /**
     * Whether to store the original message payload.
     */
    @NotNull
    private Boolean storePayload = true;

    /**
     * Whether to store exception stack traces.
     */
    @NotNull
    private Boolean storeStackTraces = true;

    /**
     * The batch size for batch operations.
     */
    @Min(1)
    @Max(10000)
    private Integer batchSize = 100;

    /**
     * The timeout for operations in milliseconds.
     */
    @Min(1000)
    @Max(60000)
    private Long operationTimeoutMs = 30000L;

    /**
     * Whether to enable automatic requeue.
     */
    @NotNull
    private Boolean autoRequeueEnabled = false;

    /**
     * The requeue interval in seconds.
     */
    @Min(10)
    @Max(3600)
    private Integer requeueIntervalSeconds = 300;

    /**
     * Whether to enable automatic purge.
     */
    @NotNull
    private Boolean autoPurgeEnabled = true;

    /**
     * The purge interval in seconds.
     */
    @Min(60)
    @Max(86400)
    private Integer purgeIntervalSeconds = 3600;

    /**
     * Whether to enable statistics tracking.
     */
    @NotNull
    private Boolean statisticsEnabled = true;

    /**
     * Whether to enable JMX monitoring.
     */
    @NotNull
    private Boolean jmxEnabled = true;

    /**
     * Topic-specific configurations.
     */
    private Map<String, TopicConfig> topics = new HashMap<>();

    /**
     * Whether to enable DLQ health checks.
     */
    @NotNull
    private Boolean healthCheckEnabled = true;

    public DeadLetterQueueConfig() {}

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTopicPrefix() {
        return topicPrefix;
    }

    public void setTopicPrefix(String topicPrefix) {
        this.topicPrefix = topicPrefix;
    }

    public Long getMaxMessages() {
        return maxMessages;
    }

    public void setMaxMessages(Long maxMessages) {
        this.maxMessages = maxMessages;
    }

    public Duration getRetentionTime() {
        return retentionTime;
    }

    public void setRetentionTime(Duration retentionTime) {
        this.retentionTime = retentionTime;
    }

    public Long getMaxPayloadSize() {
        return maxPayloadSize;
    }

    public void setMaxPayloadSize(Long maxPayloadSize) {
        this.maxPayloadSize = maxPayloadSize;
    }

    public Boolean getStorePayload() {
        return storePayload;
    }

    public void setStorePayload(Boolean storePayload) {
        this.storePayload = storePayload;
    }

    public Boolean getStoreStackTraces() {
        return storeStackTraces;
    }

    public void setStoreStackTraces(Boolean storeStackTraces) {
        this.storeStackTraces = storeStackTraces;
    }

    public Integer getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(Integer batchSize) {
        this.batchSize = batchSize;
    }

    public Long getOperationTimeoutMs() {
        return operationTimeoutMs;
    }

    public void setOperationTimeoutMs(Long operationTimeoutMs) {
        this.operationTimeoutMs = operationTimeoutMs;
    }

    public Boolean getAutoRequeueEnabled() {
        return autoRequeueEnabled;
    }

    public void setAutoRequeueEnabled(Boolean autoRequeueEnabled) {
        this.autoRequeueEnabled = autoRequeueEnabled;
    }

    public Integer getRequeueIntervalSeconds() {
        return requeueIntervalSeconds;
    }

    public void setRequeueIntervalSeconds(Integer requeueIntervalSeconds) {
        this.requeueIntervalSeconds = requeueIntervalSeconds;
    }

    public Boolean getAutoPurgeEnabled() {
        return autoPurgeEnabled;
    }

    public void setAutoPurgeEnabled(Boolean autoPurgeEnabled) {
        this.autoPurgeEnabled = autoPurgeEnabled;
    }

    public Integer getPurgeIntervalSeconds() {
        return purgeIntervalSeconds;
    }

    public void setPurgeIntervalSeconds(Integer purgeIntervalSeconds) {
        this.purgeIntervalSeconds = purgeIntervalSeconds;
    }

    public Boolean getStatisticsEnabled() {
        return statisticsEnabled;
    }

    public void setStatisticsEnabled(Boolean statisticsEnabled) {
        this.statisticsEnabled = statisticsEnabled;
    }

    public Boolean getJmxEnabled() {
        return jmxEnabled;
    }

    public void setJmxEnabled(Boolean jmxEnabled) {
        this.jmxEnabled = jmxEnabled;
    }

    public Map<String, TopicConfig> getTopics() {
        return topics;
    }

    public void setTopics(Map<String, TopicConfig> topics) {
        this.topics = topics != null ? topics : new HashMap<>();
    }

    public Boolean getHealthCheckEnabled() {
        return healthCheckEnabled;
    }

    public void setHealthCheckEnabled(Boolean healthCheckEnabled) {
        this.healthCheckEnabled = healthCheckEnabled;
    }

    /**
     * Gets topic configuration for a specific topic.
     *
     * @param topic the topic name
     * @return the topic configuration (may be null)
     */
    public TopicConfig getTopicConfig(String topic) {
        return topics.get(topic);
    }

    /**
     * Adds a topic configuration.
     *
     * @param topic the topic name
     * @param config the configuration
     */
    public void addTopicConfig(String topic, TopicConfig config) {
        if (topics == null) {
            topics = new HashMap<>();
        }
        topics.put(topic, config);
    }

    /**
     * Configuration for a specific topic.
     */
    public static class TopicConfig {

        /**
         * Whether to enable DLQ for this topic.
         */
        @NotNull
        private Boolean enabled = true;

        /**
         * The maximum number of messages for this topic.
         */
        private Long maxMessages;

        /**
         * The retention time for this topic.
         */
        private Duration retentionTime;

        /**
         * Whether to store the original payload for this topic.
         */
        private Boolean storePayload;

        /**
         * Whether to store exception stack traces for this topic.
         */
        private Boolean storeStackTraces;

        /**
         * Additional properties for this topic.
         */
        private Map<String, String> properties = new HashMap<>();

        public TopicConfig() {}

        public Boolean getEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }

        public Long getMaxMessages() {
            return maxMessages;
        }

        public void setMaxMessages(Long maxMessages) {
            this.maxMessages = maxMessages;
        }

        public Duration getRetentionTime() {
            return retentionTime;
        }

        public void setRetentionTime(Duration retentionTime) {
            this.retentionTime = retentionTime;
        }

        public Boolean getStorePayload() {
            return storePayload;
        }

        public void setStorePayload(Boolean storePayload) {
            this.storePayload = storePayload;
        }

        public Boolean getStoreStackTraces() {
            return storeStackTraces;
        }

        public void setStoreStackTraces(Boolean storeStackTraces) {
            this.storeStackTraces = storeStackTraces;
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
