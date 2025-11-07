package com.droid.bss.infrastructure.streaming;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * CDC (Change Data Capture) Streaming Service
 * Manages logical replication, event streaming, and Kafka integration
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CDCStreamingService {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Get CDC events for a specific consumer group
     */
    public List<CDCEvent> getCdcEvents(String consumerGroup, int batchSize) {
        log.debug("Fetching CDC events for consumer group: {}", consumerGroup);

        String query = """
            SELECT * FROM get_cdc_events_for_kafka(?, ?)
 """;

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(query, consumerGroup, batchSize);

        return rows.stream()
                .map(this::mapToCDCEvent)
                .collect(Collectors.toList());
    }

    /**
     * Mark CDC events as processed
     */
    public void markCdcEventsProcessed(List<UUID> eventIds) {
        log.debug("Marking {} CDC events as processed", eventIds.size());

        if (eventIds.isEmpty()) {
            return;
        }

        String query = "SELECT mark_cdc_events_processed(?)";
        UUID[] eventIdArray = eventIds.toArray(new UUID[0]);
        jdbcTemplate.update(query, (Object) eventIdArray);
    }

    /**
     * Get CDC event statistics
     */
    public List<CDCEventStats> getCdcEventStats() {
        log.debug("Fetching CDC event statistics");

        String query = """
            SELECT * FROM cdc_event_stats ORDER BY table_name, event_type
 """;

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(query);

        return rows.stream()
                .map(row -> CDCEventStats.builder()
                        .tableName(getString(row, "table_name"))
                        .eventType(getString(row, "event_type"))
                        .eventCount(getLong(row, "event_count"))
                        .ProcessedCount(getLong(row, "processed_count"))
                        .pendingCount(getLong(row, "pending_count"))
                        .firstEvent(getInstant(row, "first_event"))
                        .lastEvent(getInstant(row, "last_event"))
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Get CDC event lag
     */
    public List<CDCEventLag> getCdcEventLag() {
        log.debug("Fetching CDC event lag");

        String query = """
            SELECT * FROM cdc_event_lag ORDER BY lag_count DESC
 """;

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(query);

        return rows.stream()
                .map(row -> CDCEventLag.builder()
                        .consumerGroup(getString(row, "consumer_group"))
                        .tableName(getString(row, "table_name"))
                        .totalEvents(getLong(row, "total_events"))
                        .pendingEvents(getLong(row, "pending_events"))
                        .maxOperationId(getLong(row, "max_operation_id"))
                        .lagCount(getLong(row, "lag_count"))
                        .maxLagDuration(getString(row, "max_lag_duration"))
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Get replication health status
     */
    public List<ReplicationHealth> getReplicationHealth() {
        log.debug("Fetching replication health status");

        String query = "SELECT * FROM replication_health";

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(query);

        return rows.stream()
                .map(row -> ReplicationHealth.builder()
                        .component(getString(row, "component"))
                        .name(getString(row, "name"))
                        .status(getString(row, "status"))
                        .checkTime(getInstant(row, "check_time"))
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Record a custom CDC event
     */
    public Long recordCdcEvent(String tableName,
                               String eventType,
                               UUID pkValue,
                               Map<String, Object> oldData,
                               Map<String, Object> newData,
                               String consumerGroup,
                               String eventTopic) {
        log.debug("Recording CDC event: {} on {}", eventType, tableName);

        String oldJson = oldData != null ? new org.json.JSONObject(oldData).toString() : null;
        String newJson = newData != null ? new org.json.JSONObject(newData).toString() : null;

        String query = """
            SELECT record_cdc_event(?, ?, ?, ?, ?, ?, ?, ?)
 """;

        return jdbcTemplate.queryForObject(query, Long.class,
                tableName, eventType, pkValue, oldJson, newJson,
                null, consumerGroup, eventTopic);
    }

    /**
     * Get event store events for an aggregate
     */
    public List<StoredEvent> getAggregateEvents(UUID aggregateId, String aggregateType) {
        log.debug("Fetching events for aggregate: {} of type {}", aggregateId, aggregateType);

        String query = """
            SELECT * FROM event_store
            WHERE aggregate_id = ? AND aggregate_type = ?
            ORDER BY processed_sequence ASC
 """;

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(query, aggregateId, aggregateType);

        return rows.stream()
                .map(this::mapToStoredEvent)
                .collect(Collectors.toList());
    }

    /**
     * Append event to event store
     */
    public UUID appendEvent(UUID aggregateId,
                           String aggregateType,
                           String eventType,
                           Map<String, Object> eventData,
                           Map<String, Object> metadata) {
        log.debug("Appending event: {} for aggregate: {}", eventType, aggregateId);

        String eventDataJson = eventData != null ? new org.json.JSONObject(eventData).toString() : "{}";
        String metadataJson = metadata != null ? new org.json.JSONObject(metadata).toString() : null;

        String query = """
            SELECT append_event(?, ?, ?, ?, ?, NULL, NULL)
 """;

        return jdbcTemplate.queryForObject(query, UUID.class,
                aggregateId, aggregateType, eventType, eventDataJson, metadataJson);
    }

    /**
     * Get Kafka producer configuration
     */
    public KafkaProducerConfig getKafkaProducerConfig(String producerName) {
        log.debug("Fetching Kafka producer config: {}", producerName);

        String query = """
            SELECT * FROM kafka_producer_config
            WHERE producer_name = ? AND is_active = TRUE
 """;

        try {
            Map<String, Object> row = jdbcTemplate.queryForMap(query, producerName);
            return mapToKafkaProducerConfig(row);
        } catch (Exception e) {
            log.warn("Kafka producer config not found: {}", producerName);
            return null;
        }
    }

    /**
     * Refresh materialized view
     */
    public void refreshCustomerAnalyticsView() {
        log.info("Refreshing customer analytics materialized view");
        String query = "SELECT refresh_customer_analytics_mv()";
        jdbcTemplate.queryForObject(query, String.class);
    }

    /**
     * Get real-time customer analytics
     */
    public List<CustomerAnalytics> getCustomerAnalytics() {
        log.debug("Fetching customer analytics");

        String query = "SELECT * FROM mv_customer_analytics ORDER BY total_revenue DESC";

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(query);

        return rows.stream()
                .map(row -> CustomerAnalytics.builder()
                        .customerId(getUUID(row, "id"))
                        .email(getString(row, "email"))
                        .createdAt(getInstant(row, "created_at"))
                        .totalOrders(getLong(row, "total_orders"))
                        .totalInvoices(getLong(row, "total_invoices"))
                        .activeSubscriptions(getLong(row, "active_subscriptions"))
                        .totalRevenue(getDouble(row, "total_revenue"))
                        .updatedAt(getInstant(row, "updated_at"))
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Get real-time revenue analytics
     */
    public List<RevenueAnalytics> getRevenueAnalytics() {
        log.debug("Fetching revenue analytics");

        String query = "SELECT * FROM mv_revenue_analytics ORDER BY revenue_date DESC LIMIT 30";

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(query);

        return rows.stream()
                .map(row -> RevenueAnalytics.builder()
                        .revenueDate(getInstant(row, "revenue_date"))
                        .invoiceCount(getLong(row, "invoice_count"))
                        .uniqueCustomers(getLong(row, "unique_customers"))
                        .dailyRevenue(getDouble(row, "daily_revenue"))
                        .avgInvoiceAmount(getDouble(row, "avg_invoice_amount"))
                        .maxInvoiceAmount(getDouble(row, "max_invoice_amount"))
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Get streaming status
     */
    public StreamingStatus getStreamingStatus() {
        log.debug("Fetching streaming status");

        // Get event stats
        List<CDCEventStats> eventStats = getCdcEventStats();

        // Get lag info
        List<CDCEventLag> lagInfo = getCdcEventLag();

        // Get replication health
        List<ReplicationHealth> health = getReplicationHealth();

        long totalEvents = eventStats.stream()
                .mapToLong(CDCEventStats::getEventCount)
                .sum();

        long totalPending = eventStats.stream()
                .mapToLong(CDCEventStats::getPendingCount)
                .sum();

        return StreamingStatus.builder()
                .totalEvents(totalEvents)
                .pendingEvents(totalPending)
                .processedEvents(totalEvents - totalPending)
                .eventStats(eventStats)
                .lagInfo(lagInfo)
                .replicationHealth(health)
                .lastUpdated(Instant.now())
                .build();
    }

    /**
     * Helper methods
     */
    private CDCEvent mapToCDCEvent(Map<String, Object> row) {
        return CDCEvent.builder()
                .eventId(getUUID(row, "event_id"))
                .eventTime(getInstant(row, "event_time"))
                .eventType(getString(row, "event_type"))
                .tableName(getString(row, "table_name"))
                .operationId(getLong(row, "operation_id"))
                .pkValue(getUUID(row, "pk_value"))
                .oldData(getString(row, "old_data"))
                .newData(getString(row, "new_data"))
                .eventTopic(getString(row, "event_topic"))
                .build();
    }

    private StoredEvent mapToStoredEvent(Map<String, Object> row) {
        return StoredEvent.builder()
                .eventId(getUUID(row, "event_id"))
                .eventType(getString(row, "event_type"))
                .eventVersion(getInt(row, "event_version"))
                .aggregateId(getUUID(row, "aggregate_id"))
                .aggregateType(getString(row, "aggregate_type"))
                .eventData(getString(row, "event_data"))
                .metadata(getString(row, "metadata"))
                .eventTime(getInstant(row, "event_time"))
                .processedSequence(getInt(row, "processed_sequence"))
                .build();
    }

    private KafkaProducerConfig mapToKafkaProducerConfig(Map<String, Object> row) {
        return KafkaProducerConfig.builder()
                .producerName(getString(row, "producer_name"))
                .kafkaBootstrapServers(getString(row, "kafka_bootstrap_servers"))
                .topicPrefix(getString(row, "topic_prefix"))
                .batchSize(getInt(row, "batch_size"))
                .lingerMs(getInt(row, "linger_ms"))
                .bufferMemory(getInt(row, "buffer_memory"))
                .compressionType(getString(row, "compression_type"))
                .retries(getInt(row, "retries"))
                .acks(getString(row, "acks"))
                .isActive(getBoolean(row, "is_active"))
                .build();
    }

    private String getString(Map<String, Object> row, String key) {
        Object value = row.get(key);
        return value != null ? value.toString() : null;
    }

    private Long getLong(Map<String, Object> row, String key) {
        Object value = row.get(key);
        return value != null ? ((Number) value).longValue() : 0L;
    }

    private Integer getInt(Map<String, Object> row, String key) {
        Object value = row.get(key);
        return value != null ? ((Number) value).intValue() : 0;
    }

    private Double getDouble(Map<String, Object> row, String key) {
        Object value = row.get(key);
        return value != null ? ((Number) value).doubleValue() : 0.0;
    }

    private Boolean getBoolean(Map<String, Object> row, String key) {
        Object value = row.get(key);
        return value != null ? (Boolean) value : false;
    }

    private UUID getUUID(Map<String, Object> row, String key) {
        Object value = row.get(key);
        if (value == null) return null;
        if (value instanceof String) {
            return UUID.fromString((String) value);
        }
        return (UUID) value;
    }

    private Instant getInstant(Map<String, Object> row, String key) {
        Object value = row.get(key);
        if (value == null) return null;
        if (value instanceof java.sql.Timestamp) {
            return ((java.sql.Timestamp) value).toInstant();
        } else if (value instanceof String) {
            return Instant.parse((String) value);
        }
        return null;
    }
}
