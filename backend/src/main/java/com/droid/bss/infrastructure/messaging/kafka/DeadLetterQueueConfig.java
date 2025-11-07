package com.droid.bss.infrastructure.messaging.kafka;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ConsumerRecordRecoverer;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Dead Letter Queue (DLQ) Configuration
 *
 * Handles failed Kafka messages by routing them to DLQ topics
 * for later analysis and reprocessing
 */
@Slf4j
@Component
public class DeadLetterQueueConfig implements ConsumerRecordRecoverer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String dlqSuffix = ".DLQ";

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    public DeadLetterQueueConfig(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void recover(ConsumerRecord<?, ?> data) {
        String originalTopic = data.topic();
        String dlqTopic = originalTopic + dlqSuffix;

        log.error("Sending failed message to DLQ: topic={}, key={}, offset={}, partition={}, error={}",
            originalTopic, data.key(), data.offset(), data.partition(), data.value());

        try {
            // Create DLQ message with error metadata
            Map<String, Object> dlqMessage = new HashMap<>();
            dlqMessage.put("originalTopic", originalTopic);
            dlqMessage.put("originalKey", data.key());
            dlqMessage.put("originalValue", data.value());
            dlqMessage.put("originalOffset", data.offset());
            dlqMessage.put("originalPartition", data.partition());
            dlqMessage.put("failedAt", System.currentTimeMillis());
            dlqMessage.put("bootstrapServers", bootstrapServers);

            ProducerRecord<String, Object> dlqRecord = new ProducerRecord<>(
                dlqTopic,
                data.key(),
                dlqMessage
            );

            // Add headers with error information
            dlqRecord.headers().add("X-Original-Topic", originalTopic.getBytes());
            dlqRecord.headers().add("X-Original-Offset", String.valueOf(data.offset()).getBytes());
            dlqRecord.headers().add("X-Original-Partition", String.valueOf(data.partition()).getBytes());
            dlqRecord.headers().add("X-Retry-Count", "0".getBytes());

            kafkaTemplate.send(dlqRecord);

            log.info("Successfully sent message to DLQ: {}", dlqTopic);

        } catch (Exception e) {
            log.error("Failed to send message to DLQ: {}", dlqTopic, e);
        }
    }

    /**
     * Get the DLQ topic name for a given topic
     */
    public String getDlqTopic(String originalTopic) {
        return originalTopic + dlqSuffix;
    }
}
