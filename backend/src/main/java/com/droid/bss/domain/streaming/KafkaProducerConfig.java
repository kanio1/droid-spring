package com.droid.bss.domain.streaming;

import lombok.Builder;
import lombok.Data;

/**
 * Kafka producer configuration
 */
@Data
@Builder
public class KafkaProducerConfig {
    private String producerName;
    private String kafkaBootstrapServers;
    private String topicPrefix;
    private Integer batchSize;
    private Integer lingerMs;
    private Integer bufferMemory;
    private String compressionType;
    private Integer retries;
    private String acks;
    private Boolean isActive;
}
