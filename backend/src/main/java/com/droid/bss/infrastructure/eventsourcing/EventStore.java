package com.droid.bss.infrastructure.eventsourcing;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Event Store using Redis Streams
 * Implements event sourcing pattern for immutable event storage
 */
@Repository
public class EventStore {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String STREAM_PREFIX = "eventstream:";

    public EventStore(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Append event to stream
     */
    public String appendEvent(String aggregateType, String aggregateId, DomainEvent event) {
        String streamKey = STREAM_PREFIX + aggregateType;

        // Create event record
        EventRecord eventRecord = EventRecord.builder()
            .eventId(event.getEventId())
            .eventType(event.getEventType())
            .aggregateType(aggregateType)
            .aggregateId(aggregateId)
            .eventData(event)
            .timestamp(event.getTimestamp())
            .version(getNextVersion(aggregateType, aggregateId))
            .build();

        // Add to stream
        redisTemplate.opsForStream().add(streamKey, eventRecord.toMap());

        return eventRecord.getEventId();
    }

    /**
     * Get events for aggregate
     */
    public List<DomainEvent> getEvents(String aggregateType, String aggregateId) {
        String streamKey = STREAM_PREFIX + aggregateType;

        List<org.springframework.data.redis.core.StreamOperations<String, Object, Object>> records =
            redisTemplate.opsForStream().range(streamKey, 0, -1);

        return records.stream()
            .map(record -> {
                EventRecord eventRecord = EventRecord.fromMap(record.getValue());
                return eventRecord.getEventData();
            })
            .collect(Collectors.toList());
    }

    /**
     * Get all events for aggregate type
     */
    public List<DomainEvent> getAllEvents(String aggregateType) {
        String streamKey = STREAM_PREFIX + aggregateType;

        List<org.springframework.data.redis.core.StreamOperations<String, Object, Object>> records =
            redisTemplate.opsForStream().range(streamKey, 0, -1);

        return records.stream()
            .map(record -> EventRecord.fromMap(record.getValue()).getEventData())
            .collect(Collectors.toList());
    }

    /**
     * Get events after version
     */
    public List<DomainEvent> getEventsFromVersion(String aggregateType, String aggregateId, int version) {
        // In production, would use Redis ZSET for efficient version-based queries
        List<DomainEvent> allEvents = getEvents(aggregateType, aggregateId);
        return allEvents.stream()
            .filter(event -> event.getVersion() >= version)
            .collect(Collectors.toList());
    }

    /**
     * Get event count for aggregate
     */
    public Long getEventCount(String aggregateType, String aggregateId) {
        String streamKey = STREAM_PREFIX + aggregateType;
        return redisTemplate.opsForStream().size(streamKey);
    }

    private int getNextVersion(String aggregateType, String aggregateId) {
        // In production, use Redis INCR for atomic version tracking
        Long count = getEventCount(aggregateType, aggregateId);
        return count.intValue() + 1;
    }

    /**
     * Replay events for aggregate (for rebuilding projections)
     */
    public void replayEvents(String aggregateType, String aggregateId, EventHandler handler) {
        List<DomainEvent> events = getEvents(aggregateType, aggregateId);
        for (DomainEvent event : events) {
            handler.handle(event);
        }
    }

    /**
     * Create snapshot of aggregate state
     */
    public void createSnapshot(String aggregateType, String aggregateId, Object state) {
        String snapshotKey = "snapshot:" + aggregateType + ":" + aggregateId;
        redisTemplate.opsForValue().set(snapshotKey, state);
    }

    /**
     * Get snapshot of aggregate state
     */
    public Object getSnapshot(String aggregateType, String aggregateId) {
        String snapshotKey = "snapshot:" + aggregateType + ":" + aggregateId;
        return redisTemplate.opsForValue().get(snapshotKey);
    }

    /**
     * Event handler interface for replay
     */
    public interface EventHandler {
        void handle(DomainEvent event);
    }
}
