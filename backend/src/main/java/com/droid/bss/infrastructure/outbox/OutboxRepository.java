package com.droid.bss.infrastructure.outbox;

import com.droid.bss.domain.outbox.OutboxEvent;
import com.droid.bss.domain.outbox.OutboxEventType;
import com.droid.bss.domain.outbox.OutboxStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Outbox Event Repository
 */
@Repository
public interface OutboxRepository extends JpaRepository<OutboxEvent, UUID> {

    /**
     * Find events by status (for processing)
     */
    List<OutboxEvent> findByStatusOrderByCreatedAtAsc(OutboxStatus status);

    /**
     * Find events by status with retry conditions
     */
    @Query("SELECT e FROM OutboxEvent e WHERE e.status = :status AND (e.nextRetryAt IS NULL OR e.nextRetryAt <= :now) ORDER BY e.createdAt ASC")
    List<OutboxEvent> findPendingEventsForProcessing(
            @Param("status") OutboxStatus status,
            @Param("now") LocalDateTime now,
            Pageable pageable);

    /**
     * Find events by aggregate ID
     */
    Page<OutboxEvent> findByAggregateId(String aggregateId, Pageable pageable);

    /**
     * Find events by type
     */
    Page<OutboxEvent> findByEventType(OutboxEventType eventType, Pageable pageable);

    /**
     * Find events by correlation ID
     */
    List<OutboxEvent> findByCorrelationId(String correlationId);

    /**
     * Find events by causation ID
     */
    List<OutboxEvent> findByCausationId(String causationId);

    /**
     * Find dead letter events
     */
    Page<OutboxEvent> findByStatus(OutboxStatus status, Pageable pageable);

    /**
     * Count events by status
     */
    @Query("SELECT COUNT(e) FROM OutboxEvent e WHERE e.status = :status")
    long countByStatus(@Param("status") OutboxStatus status);

    /**
     * Count events by type
     */
    @Query("SELECT COUNT(e) FROM OutboxEvent e WHERE e.eventType = :eventType")
    long countByEventType(@Param("eventType") OutboxEventType eventType);

    /**
     * Get events published in time range
     */
    @Query("SELECT e FROM OutboxEvent e WHERE e.status = 'PUBLISHED' AND e.publishedAt BETWEEN :start AND :end ORDER BY e.publishedAt DESC")
    Page<OutboxEvent> findPublishedEventsInRange(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            Pageable pageable);

    /**
     * Mark event as published (optimistic lock)
     */
    @Modifying
    @Query("UPDATE OutboxEvent e SET e.status = 'PUBLISHED', e.publishedAt = :publishedAt, e.errorMessage = NULL WHERE e.id = :id AND e.status = 'PENDING'")
    int markAsPublished(@Param("id") UUID id, @Param("publishedAt") LocalDateTime publishedAt);

    /**
     * Mark event as failed with retry
     */
    @Modifying
    @Query("UPDATE OutboxEvent e SET e.status = :newStatus, e.retryCount = e.retryCount + 1, e.errorMessage = :errorMessage, e.nextRetryAt = :nextRetryAt WHERE e.id = :id")
    int markAsFailed(
            @Param("id") UUID id,
            @Param("newStatus") OutboxStatus newStatus,
            @Param("errorMessage") String errorMessage,
            @Param("nextRetryAt") LocalDateTime nextRetryAt);

    /**
     * Get statistics
     */
    @Query("SELECT e.status, COUNT(e) FROM OutboxEvent e GROUP BY e.status")
    List<Object[]> getStatusStatistics();

    /**
     * Find failed events for manual review
     */
    @Query("SELECT e FROM OutboxEvent e WHERE e.status = 'DEAD_LETTER' ORDER BY e.createdAt DESC")
    List<OutboxEvent> findDeadLetterEvents(Pageable pageable);

    /**
     * Find by event ID
     */
    Optional<OutboxEvent> findByEventId(String eventId);

    /**
     * Clean up old published events
     */
    @Modifying
    @Query("DELETE FROM OutboxEvent e WHERE e.status = 'PUBLISHED' AND e.publishedAt < :cutoffDate")
    int deletePublishedEventsOlderThan(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Clean up old dead letter events
     */
    @Modifying
    @Query("DELETE FROM OutboxEvent e WHERE e.status = 'DEAD_LETTER' AND e.updatedAt < :cutoffDate")
    int deleteDeadLetterEventsOlderThan(@Param("cutoffDate") LocalDateTime cutoffDate);
}
