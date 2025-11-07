package com.droid.bss.application.service;

import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

/**
 * Service to manage Kafka Streams lifecycle
 */
@Service
public class KafkaStreamsService {

    private static final Logger log = LoggerFactory.getLogger(KafkaStreamsService.class);

    private final StreamsBuilderFactoryBean streamsBuilderFactoryBean;
    private KafkaStreams streams;

    public KafkaStreamsService(StreamsBuilderFactoryBean streamsBuilderFactoryBean) {
        this.streamsBuilderFactoryBean = streamsBuilderFactoryBean;
    }

    @PostConstruct
    public void startStreams() {
        try {
            streamsBuilderFactoryBean.start();
            streams = streamsBuilderFactoryBean.getKafkaStreams();

            streams.setStateListener((newState, oldState) -> {
                log.info("Kafka Streams state changed from {} to {}", oldState, newState);
                if (newState == KafkaStreams.State.ERROR) {
                    log.error("Kafka Streams in error state");
                }
            });

            log.info("Kafka Streams started successfully");
        } catch (Exception e) {
            log.error("Failed to start Kafka Streams", e);
        }
    }

    @PreDestroy
    public void stopStreams() {
        if (streams != null) {
            log.info("Stopping Kafka Streams");
            streams.close();
            log.info("Kafka Streams stopped");
        }
    }

    public CompletableFuture<Collection<StreamsMetadata>> getAllMetadata() {
        if (streams == null) {
            return CompletableFuture.completedFuture(Collection.empty());
        }
        return CompletableFuture.completedFuture(streams.allMetadata());
    }

    public String getApplicationId() {
        return streamsBuilderFactoryBean.getStreamsConfiguration()
            .getProperty("application.id");
    }

    public boolean isRunning() {
        return streams != null && streams.state().isRunning();
    }

    public void cleanUp() {
        if (streams != null) {
            log.info("Cleaning up Kafka Streams local state");
            streams.cleanUp();
        }
    }
}
