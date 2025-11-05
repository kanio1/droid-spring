package com.droid.bss.infrastructure.event.publisher;

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
 * Configuration properties for event publishing.
 *
 * @since 1.0
 */
@Validated
@ConfigurationProperties(prefix = "app.event")
public class EventProperties {

    /**
     * Whether event publishing is enabled.
     */
    @NotNull
    private Boolean enabled = true;

    /**
     * The default event source (URN format).
     */
    @NotBlank
    private String source = "urn:droid:bss:event-publisher";

    /**
     * The default topic prefix.
     */
    @NotBlank
    private String topicPrefix = "bss.events";

    /**
     * The number of parallel publishing threads.
     */
    @Min(1)
    @Max(100)
    private Integer parallelism = 5;

    /**
     * The batch size for batch publishing.
     */
    @Min(1)
    @Max(10000)
    private Integer batchSize = 100;

    /**
     * The timeout for async operations.
     */
    @NotNull
    private Duration asyncTimeout = Duration.ofSeconds(30);

    /**
     * The buffer size for event batching.
     */
    @Min(1)
    @Max(100000)
    private Integer bufferSize = 1000;

    /**
     * The flush interval for buffered events.
     */
    @NotNull
    private Duration flushInterval = Duration.ofSeconds(5);

    /**
     * Whether to enable buffering.
     */
    @NotNull
    private Boolean bufferingEnabled = false;

    /**
     * Whether to enable event deduplication.
     */
    @NotNull
    private Boolean deduplicationEnabled = true;

    /**
     * The TTL for deduplication cache.
     */
    @NotNull
    private Duration deduplicationTtl = Duration.ofMinutes(60);

    /**
     * Whether to enable retry mechanism.
     */
    @NotNull
    private Boolean retryEnabled = true;

    /**
     * The maximum number of retries.
     */
    @Min(0)
    @Max(10)
    private Integer maxRetries = 3;

    /**
     * The retry backoff delay.
     */
    @NotNull
    private Duration retryDelay = Duration.ofSeconds(1);

    /**
     * Topic-specific configurations.
     */
    private Map<String, TopicConfig> topics = new HashMap<>();

    /**
     * Default constructor.
     */
    public EventProperties() {}

    /**
     * Constructor with all properties.
     *
     * @param enabled whether publishing is enabled
     * @param source the default source
     * @param topicPrefix the default topic prefix
     * @param parallelism the number of parallel threads
     * @param batchSize the batch size
     * @param asyncTimeout the async timeout
     */
    public EventProperties(Boolean enabled, String source, String topicPrefix, Integer parallelism,
                          Integer batchSize, Duration asyncTimeout) {
        this.enabled = enabled;
        this.source = source;
        this.topicPrefix = topicPrefix;
        this.parallelism = parallelism;
        this.batchSize = batchSize;
        this.asyncTimeout = asyncTimeout;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getTopicPrefix() {
        return topicPrefix;
    }

    public void setTopicPrefix(String topicPrefix) {
        this.topicPrefix = topicPrefix;
    }

    public Integer getParallelism() {
        return parallelism;
    }

    public void setParallelism(Integer parallelism) {
        this.parallelism = parallelism;
    }

    public Integer getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(Integer batchSize) {
        this.batchSize = batchSize;
    }

    public Duration getAsyncTimeout() {
        return asyncTimeout;
    }

    public void setAsyncTimeout(Duration asyncTimeout) {
        this.asyncTimeout = asyncTimeout;
    }

    public Integer getBufferSize() {
        return bufferSize;
    }

    public void setBufferSize(Integer bufferSize) {
        this.bufferSize = bufferSize;
    }

    public Duration getFlushInterval() {
        return flushInterval;
    }

    public void setFlushInterval(Duration flushInterval) {
        this.flushInterval = flushInterval;
    }

    public Boolean getBufferingEnabled() {
        return bufferingEnabled;
    }

    public void setBufferingEnabled(Boolean bufferingEnabled) {
        this.bufferingEnabled = bufferingEnabled;
    }

    public Boolean getDeduplicationEnabled() {
        return deduplicationEnabled;
    }

    public void setDeduplicationEnabled(Boolean deduplicationEnabled) {
        this.deduplicationEnabled = deduplicationEnabled;
    }

    public Duration getDeduplicationTtl() {
        return deduplicationTtl;
    }

    public void setDeduplicationTtl(Duration deduplicationTtl) {
        this.deduplicationTtl = deduplicationTtl;
    }

    public Boolean getRetryEnabled() {
        return retryEnabled;
    }

    public void setRetryEnabled(Boolean retryEnabled) {
        this.retryEnabled = retryEnabled;
    }

    public Integer getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(Integer maxRetries) {
        this.maxRetries = maxRetries;
    }

    public Duration getRetryDelay() {
        return retryDelay;
    }

    public void setRetryDelay(Duration retryDelay) {
        this.retryDelay = retryDelay;
    }

    public Map<String, TopicConfig> getTopics() {
        return topics;
    }

    public void setTopics(Map<String, TopicConfig> topics) {
        this.topics = topics != null ? topics : new HashMap<>();
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
         * The number of partitions for the topic.
         */
        @Min(1)
        @Max(100)
        private Integer partitions = 3;

        /**
         * The replication factor for the topic.
         */
        @Min(1)
        @Max(10)
        private Integer replicationFactor = 3;

        /**
         * The retention time for the topic.
         */
        @NotNull
        private Duration retentionTime = Duration.ofDays(7);

        /**
         * Whether to compact the topic.
         */
        @NotNull
        private Boolean compactionEnabled = false;

        /**
         * The minimum in-sync replicas.
         */
        @Min(1)
        private Integer minInSyncReplicas = 2;

        /**
         * Additional properties for the topic.
         */
        private Map<String, String> properties = new HashMap<>();

        public TopicConfig() {}

        public TopicConfig(Integer partitions, Integer replicationFactor, Duration retentionTime,
                          Boolean compactionEnabled) {
            this.partitions = partitions;
            this.replicationFactor = replicationFactor;
            this.retentionTime = retentionTime;
            this.compactionEnabled = compactionEnabled;
        }

        public Integer getPartitions() {
            return partitions;
        }

        public void setPartitions(Integer partitions) {
            this.partitions = partitions;
        }

        public Integer getReplicationFactor() {
            return replicationFactor;
        }

        public void setReplicationFactor(Integer replicationFactor) {
            this.replicationFactor = replicationFactor;
        }

        public Duration getRetentionTime() {
            return retentionTime;
        }

        public void setRetentionTime(Duration retentionTime) {
            this.retentionTime = retentionTime;
        }

        public Boolean getCompactionEnabled() {
            return compactionEnabled;
        }

        public void setCompactionEnabled(Boolean compactionEnabled) {
            this.compactionEnabled = compactionEnabled;
        }

        public Integer getMinInSyncReplicas() {
            return minInSyncReplicas;
        }

        public void setMinInSyncReplicas(Integer minInSyncReplicas) {
            this.minInSyncReplicas = minInSyncReplicas;
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
    }
}
