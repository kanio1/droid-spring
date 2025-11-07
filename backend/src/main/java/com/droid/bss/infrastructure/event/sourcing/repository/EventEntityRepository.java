package com.droid.bss.infrastructure.event.sourcing.repository;

import com.droid.bss.infrastructure.event.sourcing.entity.EventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for event entities
 */
@Repository
public interface EventEntityRepository extends JpaRepository<EventEntity, UUID> {

    /**
     * Find all events for an aggregate
     */
    @Query("SELECT e FROM EventEntity e WHERE e.aggregateId = :aggregateId ORDER BY e.version ASC")
    List<EventEntity> findByAggregateIdOrderByVersion(@Param("aggregateId") String aggregateId);

    /**
     * Find events for an aggregate with version greater than
     */
    @Query("SELECT e FROM EventEntity e WHERE e.aggregateId = :aggregateId AND e.version > :version ORDER BY e.version ASC")
    List<EventEntity> findByAggregateIdAndVersionGreaterThan(
            @Param("aggregateId") String aggregateId,
            @Param("version") long version);

    /**
     * Find events by type
     */
    @Query("SELECT e FROM EventEntity e WHERE e.eventType = :eventType ORDER BY e.timestamp DESC")
    List<EventEntity> findByEventType(@Param("eventType") String eventType);

    /**
     * Find events by correlation ID
     */
    @Query("SELECT e FROM EventEntity e WHERE e.correlationId = :correlationId ORDER BY e.timestamp ASC")
    List<EventEntity> findByCorrelationId(@Param("correlationId") String correlationId);

    /**
     * Get latest version for an aggregate
     */
    @Query("SELECT COALESCE(MAX(e.version), 0) FROM EventEntity e WHERE e.aggregateId = :aggregateId")
    long getLatestVersion(@Param("aggregateId") String aggregateId);

    /**
     * Check if aggregate exists
     */
    @Query("SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END FROM EventEntity e WHERE e.aggregateId = :aggregateId")
    boolean existsByAggregateId(@Param("aggregateId") String aggregateId);

    /**
     * Find events after a specific event ID
     */
    @Query("SELECT e FROM EventEntity e WHERE e.id > :eventId ORDER BY e.id ASC")
    List<EventEntity> findEventsAfterId(@Param("eventId") UUID eventId);

    /**
     * Count events for an aggregate
     */
    @Query("SELECT COUNT(e) FROM EventEntity e WHERE e.aggregateId = :aggregateId")
    long countByAggregateId(@Param("aggregateId") String aggregateId);
}
