package com.droid.bss.infrastructure.audit;

import com.droid.bss.domain.audit.AuditAction;
import com.droid.bss.domain.audit.AuditLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Audit Service
 *
 * Provides comprehensive audit logging capabilities
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditRepository auditRepository;

    /**
     * Log an audit event
     */
    @Transactional
    public AuditLog logEvent(AuditEvent event) {
        AuditLog auditLog = AuditLog.builder()
                .timestamp(LocalDateTime.now())
                .userId(event.getUserId())
                .username(event.getUsername())
                .action(event.getAction())
                .entityType(event.getEntityType())
                .entityId(event.getEntityId())
                .description(event.getDescription())
                .oldValues(event.getOldValues())
                .newValues(event.getNewValues())
                .metadata(event.getMetadata())
                .ipAddress(event.getIpAddress())
                .userAgent(event.getUserAgent())
                .sessionId(event.getSessionId())
                .requestId(event.getRequestId())
                .success(event.isSuccess())
                .errorMessage(event.getErrorMessage())
                .executionTimeMs(event.getExecutionTimeMs())
                .correlationId(event.getCorrelationId())
                .source(event.getSource())
                .version(event.getVersion())
                .build();

        AuditLog saved = auditRepository.save(auditLog);
        log.debug("Audit log created: {}", saved.getId());
        return saved;
    }

    /**
     * Log a successful operation
     */
    @Transactional
    public AuditLog logSuccess(AuditEvent event) {
        event.setSuccess(true);
        return logEvent(event);
    }

    /**
     * Log a failed operation
     */
    @Transactional
    public AuditLog logFailure(AuditEvent event, String errorMessage) {
        event.setSuccess(false);
        event.setErrorMessage(errorMessage);
        return logEvent(event);
    }

    /**
     * Execute with audit logging
     */
    @Transactional
    public <T> T executeWithAudit(AuditEvent event, Supplier<T> operation) {
        long startTime = System.currentTimeMillis();
        try {
            T result = operation.get();
            event.setSuccess(true);
            event.setExecutionTimeMs(System.currentTimeMillis() - startTime);
            logEvent(event);
            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            logFailure(event, e.getMessage());
            throw e;
        }
    }

    /**
     * Execute operation with audit logging (void return)
     */
    @Transactional
    public void executeWithAudit(AuditEvent event, Runnable operation) {
        executeWithAudit(event, () -> {
            operation.run();
            return null;
        });
    }

    /**
     * Query methods
     */
    public Page<AuditLog> findByUserId(String userId, Pageable pageable) {
        return auditRepository.findByUserId(userId, pageable);
    }

    public Page<AuditLog> findByAction(AuditAction action, Pageable pageable) {
        return auditRepository.findByAction(action, pageable);
    }

    public Page<AuditLog> findByEntity(String entityType, String entityId, Pageable pageable) {
        return auditRepository.findByEntityTypeAndEntityId(entityType, entityId, pageable);
    }

    public Page<AuditLog> findByTimeRange(LocalDateTime start, LocalDateTime end, Pageable pageable) {
        return auditRepository.findByTimestampBetween(start, end, pageable);
    }

    public Optional<AuditLog> findById(UUID id) {
        return auditRepository.findById(id);
    }

    /**
     * Get recent audit logs
     */
    public Page<AuditLog> getRecentLogs(int hours, Pageable pageable) {
        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        return auditRepository.findByTimestampBetween(since, LocalDateTime.now(), pageable);
    }
}
