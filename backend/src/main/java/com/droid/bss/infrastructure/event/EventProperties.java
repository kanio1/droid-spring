package com.droid.bss.infrastructure.event;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Stub class for EventProperties
 * Minimal implementation for testing purposes
 */
@Component
@ConfigurationProperties(prefix = "event")
public class EventProperties {

    private String kafkaBootstrapServers = "localhost:9092";
    private String topicPrefix = "bss";

    public String getKafkaBootstrapServers() {
        return kafkaBootstrapServers;
    }

    public void setKafkaBootstrapServers(String kafkaBootstrapServers) {
        this.kafkaBootstrapServers = kafkaBootstrapServers;
    }

    public String getTopicPrefix() {
        return topicPrefix;
    }

    public void setTopicPrefix(String topicPrefix) {
        this.topicPrefix = topicPrefix;
    }
}
