package com.droid.bss.infrastructure.messaging.deadletter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Spring configuration for Dead Letter Queue.
 *
 * @since 1.0
 */
@Configuration
@EnableConfigurationProperties(DeadLetterQueueConfig.class)
@ConditionalOnProperty(name = "app.dlq.enabled", havingValue = "true", matchIfMissing = true)
public class DeadLetterQueueConfigClass {

    private final DeadLetterQueueConfig dlqConfig;

    public DeadLetterQueueConfigClass(DeadLetterQueueConfig dlqConfig) {
        this.dlqConfig = dlqConfig;
    }

    /**
     * Creates the Kafka producer for DLQ.
     *
     * @param producerFactory the producer factory
     * @return the producer
     */
    @Bean
    @ConditionalOnMissingBean
    public KafkaProducer<String, String> dlqProducer(ProducerFactory<String, String> producerFactory) {
        return new KafkaProducer<>(producerFactory);
    }

    /**
     * Creates the producer factory for DLQ.
     *
     * @return the producer factory
     */
    @Bean
    @ConditionalOnMissingBean
    public ProducerFactory<String, String> dlqProducerFactory() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configs.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configs.put(ProducerConfig.ACKS_CONFIG, "all");
        configs.put(ProducerConfig.RETRIES_CONFIG, 3);

        return new DefaultKafkaProducerFactory<>(configs);
    }

    /**
     * Creates the DLQ producer template.
     *
     * @param producerFactory the producer factory
     * @return the Kafka template
     */
    @Bean
    @ConditionalOnMissingBean
    public KafkaTemplate<String, String> dlqTemplate(ProducerFactory<String, String> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }

    /**
     * Creates the DLQ consumer factory.
     *
     * @return the consumer factory
     */
    @Bean
    @ConditionalOnMissingBean
    public ConsumerFactory<String, String> dlqConsumerFactory() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configs.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configs.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        configs.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        return new DefaultKafkaConsumerFactory<>(configs);
    }

    /**
     * Creates the DLQ listener container factory.
     *
     * @param consumerFactory the consumer factory
     * @return the listener container factory
     */
    @Bean
    @ConditionalOnMissingBean
    public ConcurrentKafkaListenerContainerFactory<String, String> dlqListenerContainerFactory(
            ConsumerFactory<String, String> consumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        return factory;
    }

    /**
     * Creates the DLQ topic (for storing dead letters).
     *
     * @return the DLQ topic bean
     */
    @Bean
    @ConditionalOnMissingBean
    public NewTopic dlqTopic() {
        return TopicBuilder.name(dlqConfig.getTopicPrefix() + ".dlq")
            .partitions(6)
            .replicas(3)
            .config("retention.ms", String.valueOf(dlqConfig.getRetentionTime().toMillis()))
            .build();
    }

    /**
     * Creates the default DLQ consumer topic.
     *
     * @return the consumer topic bean
     */
    @Bean
    @ConditionalOnMissingBean
    public NewTopic dlqConsumerTopic() {
        return TopicBuilder.name(dlqConfig.getTopicPrefix() + ".consumer")
            .partitions(3)
            .replicas(3)
            .build();
    }

    /**
     * Creates the DLQ implementation.
     *
     * @param config the DLQ configuration
     * @param producer the Kafka producer
     * @param objectMapper the object mapper
     * @return the DLQ
     */
    @Bean
    @ConditionalOnMissingBean
    public DeadLetterQueue deadLetterQueue(DeadLetterQueueConfig config,
                                           KafkaProducer<String, String> producer,
                                           ObjectMapper objectMapper) {
        return new KafkaDeadLetterQueue(config, producer, objectMapper);
    }

    /**
     * Creates the default retry policy.
     *
     * @return the retry policy
     */
    @Bean
    @ConditionalOnMissingBean
    public RetryPolicy retryPolicy() {
        return new FixedDelayRetryPolicy(3, 1000, true);
    }

    /**
     * Creates the DLQ message processor.
     *
     * @param deadLetterQueue the DLQ
     * @param retryPolicy the retry policy
     * @return the processor
     */
    @Bean
    @ConditionalOnMissingBean
    public DLQMessageProcessor dlqMessageProcessor(DeadLetterQueue deadLetterQueue,
                                                     RetryPolicy retryPolicy) {
        return new DLQMessageProcessor(deadLetterQueue, retryPolicy);
    }

    /**
     * Creates the DLQ statistics aggregator.
     *
     * @param deadLetterQueue the DLQ
     * @return the statistics aggregator
     */
    @Bean
    @ConditionalOnMissingBean
    public DLQStatisticsAggregator dlqStatisticsAggregator(DeadLetterQueue deadLetterQueue) {
        return new DLQStatisticsAggregator(deadLetterQueue);
    }
}
