package com.droid.bss.infrastructure.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

/**
 * Security & Compliance Service
 * Manages RLS, encryption, audit logging, and compliance reporting
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SecurityComplianceService {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Get comprehensive security metrics
     */
    public SecurityMetrics getSecurityMetrics() {
        log.debug("Fetching security metrics");

        String query = "SELECT * FROM security_metrics";
        Map<String, Object> row = jdbcTemplate.queryForMap(query);

        return SecurityMetrics.builder()
                .encryptedColumns(getLong(row, "encrypted_columns"))
                .totalClassifiedColumns(getLong(row, "total_classified_columns"))
                .encryptionCoveragePercent(getDouble(row, "encryption_coverage_percent"))
                .eventsLast24h(getLong(row, "events_last_24h"))
                .eventsLast7d(getLong(row, "events_last_7d"))
                .failedEvents24h(getLong(row, "failed_events_24h"))
                .rlsEnabledTables(getLong(row, "rls_enabled_tables"))
                .totalTables(getLong(row, "total_tables"))
                .rlsCoveragePercent(getDouble(row, "rls_coverage_percent"))
                .build();
    }

    /**
     * Get compliance summary
     */
    public List<ComplianceCheck> getComplianceSummary() {
        log.debug("Fetching compliance summary");

        String query = "SELECT * FROM compliance_summary ORDER BY check_type";
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(query);

        return rows.stream()
                .map(row -> ComplianceCheck.builder()
                        .checkType(getString(row, "check_type"))
                        .totalChecks(getLong(row, "total_checks"))
                        .passed(getLong(row, "passed"))
                        .failed(getLong(row, "failed"))
                        .compliancePercent(calculateCompliancePercent(
                                getLong(row, "total_checks"),
                                getLong(row, "passed")))
                        .build())
                .collect(ArrayList::new, (list, check) -> list.add(check), ArrayList::addAll);
    }

    /**
     * Get audit log entries with filtering
     */
    public List<AuditLogEntry> getAuditLogs(String eventType,
                                             String tableName,
                                             Instant startTime,
                                             Instant endTime,
                                             int limit) {
        log.debug("Fetching audit logs with filters");

        StringBuilder query = new StringBuilder(
            "SELECT * FROM audit_log WHERE 1=1"
        );
        List<Object> params = new ArrayList<>();

        if (eventType != null) {
            query.append(" AND event_type = ?");
            params.add(eventType);
        }

        if (tableName != null) {
            query.append(" AND table_name = ?");
            params.add(tableName);
        }

        if (startTime != null) {
            query.append(" AND event_time >= ?");
            params.add(startTime);
        }

        if (endTime != null) {
            query.append(" AND event_time <= ?");
            params.add(endTime);
        }

        query.append(" ORDER BY event_time DESC LIMIT ?");
        params.add(limit);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                query.toString(), params.toArray());

        return rows.stream()
                .map(this::mapToAuditLogEntry)
                .collect(ArrayList::new, (list, entry) -> list.add(entry), ArrayList::addAll);
    }

    /**
     * Get data classification status
     */
    public List<DataClassificationStatus> getDataClassificationStatus() {
        log.debug("Fetching data classification status");

        String query = """
            SELECT
                dc.table_name,
                dc.column_name,
                dc.classification_level,
                dc.pii_type,
                dc.encrypted,
                dc.masked,
                dc.retention_period,
                ek.key_name as encryption_key
            FROM data_classification dc
            LEFT JOIN encryption_keys ek ON dc.encrypted_with_key_id = ek.key_id
            ORDER BY dc.table_name, dc.column_name
 """;

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(query);

        return rows.stream()
                .map(row -> DataClassificationStatus.builder()
                        .tableName(getString(row, "table_name"))
                        .columnName(getString(row, "column_name"))
                        .classificationLevel(getString(row, "classification_level"))
                        .piiType(getString(row, "pii_type"))
                        .isEncrypted(getBoolean(row, "encrypted"))
                        .isMasked(getBoolean(row, "masked"))
                        .retentionPeriod(getString(row, "retention_period"))
                        .encryptionKey(getString(row, "encryption_key"))
                        .build())
                .collect(ArrayList::new, (list, status) -> list.add(status), ArrayList::addAll);
    }

    /**
     * Encrypt sensitive data
     */
    public void encryptColumnData(String tableName, String columnName, String value) {
        log.info("Encrypting data in {}.{}", tableName, columnName);

        String query = "SELECT encrypt_column_data(?, ?, ?)";
        jdbcTemplate.queryForObject(query, String.class, tableName, columnName, value);
    }

    /**
     * Decrypt sensitive data
     */
    public String decryptColumnData(String tableName, String columnName, String encryptedValue) {
        log.debug("Decrypting data from {}.{}", tableName, columnName);

        String query = "SELECT decrypt_column_data(?, ?, ?)";
        return jdbcTemplate.queryForObject(query, String.class, tableName, columnName, encryptedValue);
    }

    /**
     * Mask sensitive data for display
     */
    public String maskSensitiveData(String value, String piiType, String classificationLevel) {
        String query = "SELECT mask_sensitive_data(?, ?, ?)";
        return jdbcTemplate.queryForObject(query, String.class, value, piiType, classificationLevel);
    }

    /**
     * Get RLS policies status
     */
    public List<RLSPolicyStatus> getRLSPoliciesStatus() {
        log.debug("Fetching RLS policies status");

        String query = """
            SELECT
                schemaname,
                tablename,
                rowsecurity as enabled,
                (SELECT COUNT(*) FROM pg_policies WHERE schemaname = t.schemaname AND tablename = t.tablename) as policy_count
            FROM pg_tables t
            WHERE schemaname = 'public'
            ORDER BY tablename
 """;

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(query);

        return rows.stream()
                .map(row -> RLSPolicyStatus.builder()
                        .schemaName(getString(row, "schemaname"))
                        .tableName(getString(row, "tablename"))
                        .isEnabled(getBoolean(row, "enabled"))
                        .policyCount(getLong(row, "policy_count"))
                        .build())
                .collect(ArrayList::new, (list, status) -> list.add(status), ArrayList::addAll);
    }

    /**
     * Enable RLS on a table
     */
    public void enableRLS(String tableName) {
        log.info("Enabling RLS on table: {}", tableName);

        String query = String.format("ALTER TABLE %s ENABLE ROW LEVEL SECURITY", tableName);
        jdbcTemplate.execute(query);
    }

    /**
     * Disable RLS on a table
     */
    public void disableRLS(String tableName) {
        log.warn("Disabling RLS on table: {}", tableName);

        String query = String.format("ALTER TABLE %s DISABLE ROW LEVEL SECURITY", tableName);
        jdbcTemplate.execute(query);
    }

    /**
     * Log a custom audit event
     */
    public UUID logCustomAuditEvent(String userId,
                                     String eventType,
                                     String tableName,
                                     String details) {
        log.debug("Logging custom audit event: {}", eventType);

        String query = """
            SELECT log_audit_event(
                ?::UUID, ?, ?, 'public', NULL, 'AFTER',
                NULL, ?::JSONB, ?, NULL, NULL, TRUE, NULL
            )
 """;

        Map<String, Object> detailsMap = Map.of("details", details);
        String detailsJson = new org.json.JSONObject(detailsMap).toString();

        return jdbcTemplate.queryForObject(query, UUID.class,
                userId, eventType, tableName, detailsJson, "");
    }

    /**
     * Clean up expired data based on retention policies
     */
    public List<String> cleanupExpiredData() {
        log.info("Starting cleanup of expired data");

        String query = "SELECT cleanup_expired_data()";
        List<String> results = jdbcTemplate.queryForList(query, String.class);

        return results;
    }

    /**
     * Get user roles
     */
    public List<String> getUserRoles(UUID userId) {
        log.debug("Fetching roles for user: {}", userId);

        String query = "SELECT role_name FROM user_roles WHERE user_id = ?";
        return jdbcTemplate.queryForList(query, String.class, userId);
    }

    /**
     * Grant role to user
     */
    public void grantRole(UUID userId, String roleName) {
        log.info("Granting role {} to user: {}", roleName, userId);

        String query = "INSERT INTO user_roles (user_id, role_name) VALUES (?, ?) ON CONFLICT DO NOTHING";
        jdbcTemplate.update(query, userId, roleName);
    }

    /**
     * Revoke role from user
     */
    public void revokeRole(UUID userId, String roleName) {
        log.info("Revoking role {} from user: {}", roleName, userId);

        String query = "DELETE FROM user_roles WHERE user_id = ? AND role_name = ?";
        jdbcTemplate.update(query, userId, roleName);
    }

    /**
     * Get failed login attempts
     */
    public List<FailedLoginAttempt> getFailedLoginAttempts(Instant since) {
        log.debug("Fetching failed login attempts since: {}", since);

        String query = """
            SELECT user_id, user_name, COUNT(*) as attempt_count, MAX(event_time) as last_attempt
            FROM audit_log
            WHERE event_type = 'LOGIN' AND success = FALSE AND event_time >= ?
            GROUP BY user_id, user_name
            ORDER BY attempt_count DESC
 """;

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(query, since);

        return rows.stream()
                .map(row -> FailedLoginAttempt.builder()
                        .userId(getUUID(row, "user_id"))
                        .userName(getString(row, "user_name"))
                        .attemptCount(getLong(row, "attempt_count"))
                        .lastAttempt(getInstant(row, "last_attempt"))
                        .build())
                .collect(ArrayList::new, (list, attempt) -> list.add(attempt), ArrayList::addAll);
    }

    /**
     * Get security alerts
     */
    public List<SecurityAlert> getSecurityAlerts(Instant since) {
        log.debug("Fetching security alerts since: {}", since);

        String query = """
            SELECT
                audit_id,
                event_time,
                user_id,
                user_name,
                event_type,
                table_name,
                ip_address,
                severity,
                error_message
            FROM audit_log
            WHERE event_time >= ?
            AND (success = FALSE OR severity IN ('WARNING', 'ERROR', 'CRITICAL'))
            ORDER BY event_time DESC
            LIMIT 100
 """;

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(query, since);

        return rows.stream()
                .map(row -> SecurityAlert.builder()
                        .alertId(getUUID(row, "audit_id"))
                        .timestamp(getInstant(row, "event_time"))
                        .userId(getUUID(row, "user_id"))
                        .userName(getString(row, "user_name"))
                        .eventType(getString(row, "event_type"))
                        .tableName(getString(row, "table_name"))
                        .ipAddress(getString(row, "ip_address"))
                        .severity(getString(row, "severity"))
                        .message(getString(row, "error_message"))
                        .build())
                .collect(ArrayList::new, (list, alert) -> list.add(alert), ArrayList::addAll);
    }

    /**
     * Helper methods
     */
    private AuditLogEntry mapToAuditLogEntry(Map<String, Object> row) {
        return AuditLogEntry.builder()
                .auditId(getUUID(row, "audit_id"))
                .eventTime(getInstant(row, "event_time"))
                .userId(getUUID(row, "user_id"))
                .userName(getString(row, "user_name"))
                .eventType(getString(row, "event_type"))
                .tableName(getString(row, "table_name"))
                .operation(getString(row, "operation"))
                .success(getBoolean(row, "success"))
                .ipAddress(getString(row, "ip_address"))
                .build();
    }

    private double calculateCompliancePercent(Long total, Long passed) {
        if (total == null || total == 0) return 0.0;
        if (passed == null) return 0.0;
        return Math.round((double) passed / total * 10000.0) / 100.0;
    }

    private String getString(Map<String, Object> row, String key) {
        Object value = row.get(key);
        return value != null ? value.toString() : null;
    }

    private Long getLong(Map<String, Object> row, String key) {
        Object value = row.get(key);
        return value != null ? ((Number) value).longValue() : 0L;
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
