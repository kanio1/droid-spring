package org.apache.kafka.common.record;

/**
 * Stub class for RecordMetadata (from kafka-clients)
 * Minimal implementation for testing purposes
 */
public class RecordMetadata {

    public int partition() {
        return 0;
    }

    public long offset() {
        return 0L;
    }

    public String topic() {
        return "test-topic";
    }
}
