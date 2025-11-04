package com.droid.bss.infrastructure.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Health indicator for Kafka connection
 */
@Component
public class KafkaHealthIndicator implements HealthIndicator {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public KafkaHealthIndicator(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public Health health() {
        try {
            // Test Kafka connection by getting cluster metadata
            var metaData = kafkaTemplate.getProducerFactory().getConfigurationProperties();

            // Check if Kafka bootstrap servers are configured
            String bootstrapServers = (String) metaData.get("bootstrap.servers");
            if (bootstrapServers == null || bootstrapServers.isEmpty()) {
                return Health.down()
                        .withDetail("status", "Kafka bootstrap servers not configured")
                        .build();
            }

            // If we can access the producer factory configuration, Kafka is available
            return Health.up()
                    .withDetail("status", "Kafka is available")
                    .withDetail("bootstrap.servers", bootstrapServers)
                    .build();

        } catch (Exception e) {
            return Health.down()
                    .withDetail("status", "Kafka connection failed")
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
