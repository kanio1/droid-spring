package com.droid.bss.infrastructure.eventsourcing;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Publishes events to Redis Streams and Kafka
 */
@Component
public class EventPublisher {

    private final EventStore eventStore;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final List<EventHandler> eventHandlers;

    public EventPublisher(EventStore eventStore, KafkaTemplate<String, Object> kafkaTemplate) {
        this.eventStore = eventStore;
        this.kafkaTemplate = kafkaTemplate;
        this.eventHandlers = new ArrayList<>();
    }

    public void registerHandler(EventHandler handler) {
        eventHandlers.add(handler);
    }

    public <T extends DomainEvent> String publish(String aggregateType, String aggregateId, T event) {
        // Store in event store
        String eventId = eventStore.appendEvent(aggregateType, aggregateId, event);

        // Publish to Redis Stream
        publishToRedisStream(aggregateType, event);

        // Publish to Kafka (for other services)
        publishToKafka(aggregateType, eventId, event);

        // Notify local handlers
        notifyHandlers(event);

        return eventId;
    }

    private void publishToRedisStream(String aggregateType, DomainEvent event) {
        // Redis Stream already managed by EventStore
        // This is a placeholder for additional stream logic
    }

    private void publishToKafka(String aggregateType, String eventId, DomainEvent event) {
        try {
            String topic = "bss.events." + aggregateType.toLowerCase();
            kafkaTemplate.send(topic, eventId, event);
        } catch (Exception e) {
            // Log but don't fail
            System.err.println("Failed to publish event to Kafka: " + e.getMessage());
        }
    }

    private void notifyHandlers(DomainEvent event) {
        for (EventHandler handler : eventHandlers) {
            try {
                handler.handle(event);
            } catch (Exception e) {
                // Log but continue
                System.err.println("Error in event handler: " + e.getMessage());
            }
        }
    }
}
