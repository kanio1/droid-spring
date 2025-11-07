package com.droid.bss.infrastructure.audit;

import com.droid.bss.domain.audit.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Audit Log Repository
 */
@Repository
public interface AuditRepository extends JpaRepository<AuditLog, UUID> {

    /**
     * Find audit logs by user
     */
    Page<AuditLog> findByUserId(String userId, Pageable pageable);

    /**
     * Find audit logs by action
     */
    Page<AuditLog> findByAction(com.droid.bss.domain.audit.AuditAction action, Pageable pageable);

    /**
     * Find audit logs by entity
     */
    Page<AuditLog> findByEntityTypeAndEntityId(String entityType, String entityId, Pageable pageable);

    /**
     * Find audit logs by time range
     */
    Page<AuditLog> findByTimestampBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);

    /**
     * Find audit logs by user and time range
     */
    @Query("SELECT a FROM AuditLog a WHERE a.userId = :userId AND a.timestamp BETWEEN :start AND :end ORDER BY a.timestamp DESC")
    Page<AuditLog> findByUserIdAndTimestampBetween(
            @Param("userId") String userId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            Pageable pageable);

    /**
     * Find audit logs by action and time range
     */
    @Query("SELECT a FROM AuditLog a WHERE a.action = :action AND a.timestamp BETWEEN :start AND :end ORDER BY a.timestamp DESC")
    Page<AuditLog> findByActionAndTimestampBetween(
            @Param("action") com.droid.bss.domain.audit.AuditAction action,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            Pageable pageable);

    /**
     * Find recent audit logs (last 24 hours)
     */
    @Query("SELECT a FROM AuditLog a WHERE a.timestamp > :since ORDER BY a.timestamp DESC")
    List<AuditLog> findRecent(@Param("since") LocalDateTime since);

    /**
     * Count actions by user
     */
    @Query("SELECT a.action, COUNT(a) FROM AuditLog a WHERE a.userId = :userId AND a.timestamp BETWEEN :start AND :end GROUP BY a.action")
    List<Object[]> countByUserAndAction(
            @Param("userId") String userId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    /**
     * Find failed operations
     */
    @Query("SELECT a FROM AuditLog a WHERE a.success = false ORDER BY a.timestamp DESC")
    List<AuditLog> findFailedOperations(Pageable pageable);

    /**
     * Find by correlation ID
     */
    List<AuditLog> findByCorrelationId(String correlationId);

    /**
     * Count logs by action
     */
    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.action = :action")
    long countByAction(@Param("action") com.droid.bss.domain.audit.AuditAction action);
}
