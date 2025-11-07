package com.droid.bss.api.streaming;

import com.droid.bss.infrastructure.streaming.CDCStreamingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * CDC Streaming API
 * Provides endpoints for real-time data streaming and change data capture
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/streaming")
@RequiredArgsConstructor
@Tag(name = "CDC Streaming", description = "Change Data Capture and real-time data streaming")
public class StreamingController {

    private final CDCStreamingService streamingService;

    @GetMapping("/status")
    @Operation(summary = "Get streaming status", description = "Returns comprehensive streaming status including CDC events, lag, and replication health")
    public ResponseEntity<StreamingStatus> getStreamingStatus() {
        log.debug("Fetching streaming status");
        StreamingStatus status = streamingService.getStreamingStatus();
        return ResponseEntity.ok(status);
    }

    @GetMapping("/events")
    @Operation(summary = "Get CDC events", description = "Returns unprocessed CDC events for a consumer group")
    public ResponseEntity<List<CDCEvent>> getCdcEvents(
            @RequestParam String consumerGroup,
            @RequestParam(defaultValue = "100") int batchSize) {

        log.debug("Fetching CDC events for consumer group: {}", consumerGroup);
        List<CDCEvent> events = streamingService.getCdcEvents(consumerGroup, batchSize);
        return ResponseEntity.ok(events);
    }

    @PostMapping("/events/processed")
    @Operation(summary = "Mark CDC events as processed", description = "Marks CDC events as processed in the event log")
    public ResponseEntity<Map<String, String>> markCdcEventsProcessed(
            @RequestBody MarkProcessedRequest request) {

        log.debug("Marking {} CDC events as processed", request.getEventIds().size());
        streamingService.markCdcEventsProcessed(request.getEventIds());

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Events marked as processed",
                "count", String.valueOf(request.getEventIds().size())
        ));
    }

    @GetMapping("/events/stats")
    @Operation(summary = "Get CDC event statistics", description = "Returns statistics about CDC events by table and event type")
    public ResponseEntity<List<CDCEventStats>> getCdcEventStats() {
        log.debug("Fetching CDC event statistics");
        List<CDCEventStats> stats = streamingService.getCdcEventStats();
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/events/lag")
    @Operation(summary = "Get CDC event lag", description = "Returns lag information for CDC events by consumer group")
    public ResponseEntity<List<CDCEventLag>> getCdcEventLag() {
        log.debug("Fetching CDC event lag");
        List<CDCEventLag> lag = streamingService.getCdcEventLag();
        return ResponseEntity.ok(lag);
    }

    @GetMapping("/replication/health")
    @Operation(summary = "Get replication health", description = "Returns replication health status")
    public ResponseEntity<List<ReplicationHealth>> getReplicationHealth() {
        log.debug("Fetching replication health");
        List<ReplicationHealth> health = streamingService.getReplicationHealth();
        return ResponseEntity.ok(health);
    }

    @PostMapping("/events/record")
    @Operation(summary = "Record custom CDC event", description = "Records a custom CDC event in the event log")
    public ResponseEntity<Map<String, String>> recordCdcEvent(
            @RequestBody RecordCdcEventRequest request) {

        log.debug("Recording CDC event: {} on {}", request.getEventType(), request.getTableName());
        Long operationId = streamingService.recordCdcEvent(
                request.getTableName(),
                request.getEventType(),
                request.getPkValue(),
                request.getOldData(),
                request.getNewData(),
                request.getConsumerGroup(),
                request.getEventTopic()
        );

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "CDC event recorded",
                "operationId", operationId.toString()
        ));
    }

    @GetMapping("/events/aggregate/{aggregateId}")
    @Operation(summary = "Get aggregate events", description = "Returns all events for a specific aggregate (event sourcing)")
    public ResponseEntity<List<StoredEvent>> getAggregateEvents(
            @PathVariable UUID aggregateId,
            @RequestParam String aggregateType) {

        log.debug("Fetching events for aggregate: {} of type {}", aggregateId, aggregateType);
        List<StoredEvent> events = streamingService.getAggregateEvents(aggregateId, aggregateType);
        return ResponseEntity.ok(events);
    }

    @PostMapping("/events/append")
    @Operation(summary = "Append event to event store", description = "Appends an event to the event store (event sourcing pattern)")
    public ResponseEntity<Map<String, String>> appendEvent(
            @RequestBody AppendEventRequest request) {

        log.debug("Appending event: {} for aggregate: {}", request.getEventType(), request.getAggregateId());
        UUID eventId = streamingService.appendEvent(
                request.getAggregateId(),
                request.getAggregateType(),
                request.getEventType(),
                request.getEventData(),
                request.getMetadata()
        );

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Event appended to store",
                "eventId", eventId.toString()
        ));
    }

    @GetMapping("/kafka/config/{producerName}")
    @Operation(summary = "Get Kafka producer config", description = "Returns Kafka producer configuration")
    public ResponseEntity<KafkaProducerConfig> getKafkaProducerConfig(@PathVariable String producerName) {
        log.debug("Fetching Kafka producer config: {}", producerName);
        KafkaProducerConfig config = streamingService.getKafkaProducerConfig(producerName);
        return ResponseEntity.ok(config);
    }

    @PostMapping("/refresh/analytics")
    @Operation(summary = "Refresh analytics materialized view", description = "Refreshes the customer analytics materialized view")
    public ResponseEntity<Map<String, String>> refreshAnalyticsView() {
        log.info("Refreshing analytics materialized view");
        streamingService.refreshCustomerAnalyticsView();

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Analytics view refreshed"
        ));
    }

    @GetMapping("/analytics/customers")
    @Operation(summary = "Get customer analytics", description = "Returns real-time customer analytics from materialized view")
    public ResponseEntity<List<CustomerAnalytics>> getCustomerAnalytics() {
        log.debug("Fetching customer analytics");
        List<CustomerAnalytics> analytics = streamingService.getCustomerAnalytics();
        return ResponseEntity.ok(analytics);
    }

    @GetMapping("/analytics/revenue")
    @Operation(summary = "Get revenue analytics", description = "Returns real-time revenue analytics from materialized view")
    public ResponseEntity<List<RevenueAnalytics>> getRevenueAnalytics() {
        log.debug("Fetching revenue analytics");
        List<RevenueAnalytics> analytics = streamingService.getRevenueAnalytics();
        return ResponseEntity.ok(analytics);
    }

    /**
     * Request DTOs
     */
    public static class MarkProcessedRequest {
        private List<UUID> eventIds;

        public List<UUID> getEventIds() { return eventIds; }
        public void setEventIds(List<UUID> eventIds) { this.eventIds = eventIds; }
    }

    public static class RecordCdcEventRequest {
        private String tableName;
        private String eventType;
        private UUID pkValue;
        private Map<String, Object> oldData;
        private Map<String, Object> newData;
        private String consumerGroup;
        private String eventTopic;

        // Getters and setters
        public String getTableName() { return tableName; }
        public void setTableName(String tableName) { this.tableName = tableName; }
        public String getEventType() { return eventType; }
        public void setEventType(String eventType) { this.eventType = eventType; }
        public UUID getPkValue() { return pkValue; }
        public void setPkValue(UUID pkValue) { this.pkValue = pkValue; }
        public Map<String, Object> getOldData() { return oldData; }
        public void setOldData(Map<String, Object> oldData) { this.oldData = oldData; }
        public Map<String, Object> getNewData() { return newData; }
        public void setNewData(Map<String, Object> newData) { this.newData = newData; }
        public String getConsumerGroup() { return consumerGroup; }
        public void setConsumerGroup(String consumerGroup) { this.consumerGroup = consumerGroup; }
        public String getEventTopic() { return eventTopic; }
        public void setEventTopic(String eventTopic) { this.eventTopic = eventTopic; }
    }

    public static class AppendEventRequest {
        private UUID aggregateId;
        private String aggregateType;
        private String eventType;
        private Map<String, Object> eventData;
        private Map<String, Object> metadata;

        // Getters and setters
        public UUID getAggregateId() { return aggregateId; }
        public void setAggregateId(UUID aggregateId) { this.aggregateId = aggregateId; }
        public String getAggregateType() { return aggregateType; }
        public void setAggregateType(String aggregateType) { this.aggregateType = aggregateType; }
        public String getEventType() { return eventType; }
        public void setEventType(String eventType) { this.eventType = eventType; }
        public Map<String, Object> getEventData() { return eventData; }
        public void setEventData(Map<String, Object> eventData) { this.eventData = eventData; }
        public Map<String, Object> getMetadata() { return metadata; }
        public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    }
}
