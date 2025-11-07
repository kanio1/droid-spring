package com.droid.bss.api.security;

import com.droid.bss.infrastructure.security.SecurityComplianceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Security & Compliance API
 * Provides endpoints for security monitoring, audit logging, and compliance reporting
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/security")
@RequiredArgsConstructor
@Tag(name = "Security & Compliance", description = "Security monitoring, audit logging, and compliance")
public class SecurityController {

    private final SecurityComplianceService securityService;

    @GetMapping("/metrics")
    @Operation(summary = "Get security metrics", description = "Returns comprehensive security metrics including encryption coverage, RLS status, and audit volume")
    public ResponseEntity<SecurityMetrics> getSecurityMetrics() {
        log.debug("Fetching security metrics");
        SecurityMetrics metrics = securityService.getSecurityMetrics();
        return ResponseEntity.ok(metrics);
    }

    @GetMapping("/compliance")
    @Operation(summary = "Get compliance summary", description = "Returns compliance status for encryption, RLS, and audit logging")
    public ResponseEntity<List<ComplianceCheck>> getComplianceSummary() {
        log.debug("Fetching compliance summary");
        List<ComplianceCheck> compliance = securityService.getComplianceSummary();
        return ResponseEntity.ok(compliance);
    }

    @GetMapping("/audit-logs")
    @Operation(summary = "Get audit logs", description = "Returns audit log entries with optional filtering by event type, table, and time range")
    public ResponseEntity<List<AuditLogEntry>> getAuditLogs(
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String tableName,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime,
            @RequestParam(defaultValue = "100") int limit) {

        log.debug("Fetching audit logs with filters");
        List<AuditLogEntry> logs = securityService.getAuditLogs(
                eventType, tableName, startTime, endTime, limit);
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/data-classification")
    @Operation(summary = "Get data classification status", description = "Returns PII data classification and encryption status")
    public ResponseEntity<List<DataClassificationStatus>> getDataClassificationStatus() {
        log.debug("Fetching data classification status");
        List<DataClassificationStatus> status = securityService.getDataClassificationStatus();
        return ResponseEntity.ok(status);
    }

    @GetMapping("/rls-policies")
    @Operation(summary = "Get RLS policies status", description = "Returns Row Level Security status for all tables")
    public ResponseEntity<List<RLSPolicyStatus>> getRLSPoliciesStatus() {
        log.debug("Fetching RLS policies status");
        List<RLSPolicyStatus> policies = securityService.getRLSPoliciesStatus();
        return ResponseEntity.ok(policies);
    }

    @PostMapping("/rls/enable/{tableName}")
    @Operation(summary = "Enable RLS on table", description = "Enables Row Level Security on a specific table")
    public ResponseEntity<Map<String, String>> enableRLS(@PathVariable String tableName) {
        log.info("Enabling RLS on table: {}", tableName);
        securityService.enableRLS(tableName);
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "RLS enabled on table: " + tableName,
                "tableName", tableName
        ));
    }

    @PostMapping("/rls/disable/{tableName}")
    @Operation(summary = "Disable RLS on table", description = "Disables Row Level Security on a specific table")
    public ResponseEntity<Map<String, String>> disableRLS(@PathVariable String tableName) {
        log.warn("Disabling RLS on table: {}", tableName);
        securityService.disableRLS(tableName);
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "RLS disabled on table: " + tableName,
                "tableName", tableName
        ));
    }

    @PostMapping("/audit/log")
    @Operation(summary = "Log custom audit event", description = "Logs a custom audit event")
    public ResponseEntity<Map<String, String>> logCustomAuditEvent(
            @RequestBody AuditLogRequest request) {
        log.debug("Logging custom audit event: {}", request.getEventType());
        UUID auditId = securityService.logCustomAuditEvent(
                request.getUserId(),
                request.getEventType(),
                request.getTableName(),
                request.getDetails()
        );
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Audit event logged",
                "auditId", auditId.toString()
        ));
    }

    @PostMapping("/cleanup")
    @Operation(summary = "Clean up expired data", description = "Executes data cleanup based on retention policies")
    public ResponseEntity<List<String>> cleanupExpiredData() {
        log.info("Starting expired data cleanup");
        List<String> results = securityService.cleanupExpiredData();
        return ResponseEntity.ok(results);
    }

    @GetMapping("/roles/{userId}")
    @Operation(summary = "Get user roles", description = "Returns roles assigned to a user")
    public ResponseEntity<List<String>> getUserRoles(@PathVariable UUID userId) {
        log.debug("Fetching roles for user: {}", userId);
        List<String> roles = securityService.getUserRoles(userId);
        return ResponseEntity.ok(roles);
    }

    @PostMapping("/roles/grant")
    @Operation(summary = "Grant role to user", description = "Assigns a role to a user")
    public ResponseEntity<Map<String, String>> grantRole(
            @RequestBody RoleManagementRequest request) {
        log.info("Granting role {} to user: {}", request.getRoleName(), request.getUserId());
        securityService.grantRole(request.getUserId(), request.getRoleName());
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Role granted successfully",
                "userId", request.getUserId().toString(),
                "role", request.getRoleName()
        ));
    }

    @PostMapping("/roles/revoke")
    @Operation(summary = "Revoke role from user", description = "Removes a role from a user")
    public ResponseEntity<Map<String, String>> revokeRole(
            @RequestBody RoleManagementRequest request) {
        log.info("Revoking role {} from user: {}", request.getRoleName(), request.getUserId());
        securityService.revokeRole(request.getUserId(), request.getRoleName());
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Role revoked successfully",
                "userId", request.getUserId().toString(),
                "role", request.getRoleName()
        ));
    }

    @GetMapping("/failed-logins")
    @Operation(summary = "Get failed login attempts", description = "Returns failed login attempts in the specified time period")
    public ResponseEntity<List<FailedLoginAttempt>> getFailedLoginAttempts(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant since) {
        log.debug("Fetching failed login attempts since: {}", since);
        List<FailedLoginAttempt> attempts = securityService.getFailedLoginAttempts(since);
        return ResponseEntity.ok(attempts);
    }

    @GetMapping("/alerts")
    @Operation(summary = "Get security alerts", description = "Returns security alerts including failed events and high-severity activities")
    public ResponseEntity<List<SecurityAlert>> getSecurityAlerts(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant since) {
        log.debug("Fetching security alerts since: {}", since);
        Instant defaultSince = since != null ? since : Instant.now().minus(24, java.time.temporal.ChronoUnit.HOURS);
        List<SecurityAlert> alerts = securityService.getSecurityAlerts(defaultSince);
        return ResponseEntity.ok(alerts);
    }

    @PostMapping("/encrypt")
    @Operation(summary = "Encrypt column data", description = "Encrypts data in a specific column")
    public ResponseEntity<Map<String, String>> encryptColumnData(
            @RequestBody EncryptRequest request) {
        log.info("Encrypting data in {}.{}", request.getTableName(), request.getColumnName());
        securityService.encryptColumnData(
                request.getTableName(),
                request.getColumnName(),
                request.getValue()
        );
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Data encrypted successfully"
        ));
    }

    @PostMapping("/decrypt")
    @Operation(summary = "Decrypt column data", description = "Decrypts data in a specific column")
    public ResponseEntity<String> decryptColumnData(
            @RequestBody DecryptRequest request) {
        log.debug("Decrypting data from {}.{}", request.getTableName(), request.getColumnName());
        String decrypted = securityService.decryptColumnData(
                request.getTableName(),
                request.getColumnName(),
                request.getEncryptedValue()
        );
        return ResponseEntity.ok(decrypted);
    }

    @PostMapping("/mask")
    @Operation(summary = "Mask sensitive data", description = "Masks sensitive data for display purposes")
    public ResponseEntity<String> maskSensitiveData(
            @RequestBody MaskRequest request) {
        log.debug("Masking sensitive data");
        String masked = securityService.maskSensitiveData(
                request.getValue(),
                request.getPiiType(),
                request.getClassificationLevel()
        );
        return ResponseEntity.ok(masked);
    }

    /**
     * Request DTOs
     */
    public static class AuditLogRequest {
        private String userId;
        private String eventType;
        private String tableName;
        private String details;

        // Getters and setters
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public String getEventType() { return eventType; }
        public void setEventType(String eventType) { this.eventType = eventType; }
        public String getTableName() { return tableName; }
        public void setTableName(String tableName) { this.tableName = tableName; }
        public String getDetails() { return details; }
        public void setDetails(String details) { this.details = details; }
    }

    public static class RoleManagementRequest {
        private UUID userId;
        private String roleName;

        // Getters and setters
        public UUID getUserId() { return userId; }
        public void setUserId(UUID userId) { this.userId = userId; }
        public String getRoleName() { return roleName; }
        public void setRoleName(String roleName) { this.roleName = roleName; }
    }

    public static class EncryptRequest {
        private String tableName;
        private String columnName;
        private String value;

        // Getters and setters
        public String getTableName() { return tableName; }
        public void setTableName(String tableName) { this.tableName = tableName; }
        public String getColumnName() { return columnName; }
        public void setColumnName(String columnName) { this.columnName = columnName; }
        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
    }

    public static class DecryptRequest {
        private String tableName;
        private String columnName;
        private String encryptedValue;

        // Getters and setters
        public String getTableName() { return tableName; }
        public void setTableName(String tableName) { this.tableName = tableName; }
        public String getColumnName() { return columnName; }
        public void setColumnName(String columnName) { this.columnName = columnName; }
        public String getEncryptedValue() { return encryptedValue; }
        public void setEncryptedValue(String encryptedValue) { this.encryptedValue = encryptedValue; }
    }

    public static class MaskRequest {
        private String value;
        private String piiType;
        private String classificationLevel;

        // Getters and setters
        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
        public String getPiiType() { return piiType; }
        public void setPiiType(String piiType) { this.piiType = piiType; }
        public String getClassificationLevel() { return classificationLevel; }
        public void setClassificationLevel(String classificationLevel) { this.classificationLevel = classificationLevel; }
    }
}
