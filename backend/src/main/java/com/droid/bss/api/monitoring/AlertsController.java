package com.droid.bss.api.monitoring;

import com.droid.bss.application.dto.monitoring.AlertOperationRequest;
import com.droid.bss.application.dto.monitoring.AlertResponse;
import com.droid.bss.application.service.monitoring.GetAlertsUseCase;
import com.droid.bss.application.service.monitoring.ManageAlertUseCase;
import com.droid.bss.domain.audit.AuditAction;
import com.droid.bss.domain.monitoring.Alert;
import com.droid.bss.infrastructure.audit.Audited;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * REST API controller for alerts
 */
@RestController
@RequestMapping("/api/v1/alerts")
@Tag(name = "Alerts", description = "Alert management API")
public class AlertsController {

    private final GetAlertsUseCase getAlertsUseCase;
    private final ManageAlertUseCase manageAlertUseCase;

    public AlertsController(GetAlertsUseCase getAlertsUseCase,
                           ManageAlertUseCase manageAlertUseCase) {
        this.getAlertsUseCase = getAlertsUseCase;
        this.manageAlertUseCase = manageAlertUseCase;
    }

    @GetMapping
    @Operation(summary = "Get alerts by customer and time range")
    public ResponseEntity<List<AlertResponse>> getAlertsByCustomer(
            @RequestParam Long customerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime) {
        List<Alert> alerts = getAlertsUseCase.getAlertsByCustomerAndTimeRange(
                customerId, startTime, endTime);
        return ResponseEntity.ok(alerts.stream().map(this::toResponse).collect(Collectors.toList()));
    }

    @GetMapping("/active")
    @Operation(summary = "Get all active alerts")
    public ResponseEntity<List<AlertResponse>> getActiveAlerts() {
        List<Alert> alerts = getAlertsUseCase.getActiveAlerts();
        return ResponseEntity.ok(alerts.stream().map(this::toResponse).collect(Collectors.toList()));
    }

    @GetMapping("/customer/{customerId}/active")
    @Operation(summary = "Get active alerts for a customer")
    public ResponseEntity<List<AlertResponse>> getActiveAlertsByCustomer(@PathVariable Long customerId) {
        List<Alert> alerts = getAlertsUseCase.getActiveAlertsByCustomer(customerId);
        return ResponseEntity.ok(alerts.stream().map(this::toResponse).collect(Collectors.toList()));
    }

    @GetMapping("/resource/{resourceId}")
    @Operation(summary = "Get alerts by resource")
    public ResponseEntity<List<AlertResponse>> getAlertsByResource(@PathVariable Long resourceId) {
        List<Alert> alerts = getAlertsUseCase.getAlertsByResource(resourceId);
        return ResponseEntity.ok(alerts.stream().map(this::toResponse).collect(Collectors.toList()));
    }

    @GetMapping("/severity/{severity}")
    @Operation(summary = "Get alerts by severity")
    public ResponseEntity<List<AlertResponse>> getAlertsBySeverity(@PathVariable String severity) {
        List<Alert> alerts = getAlertsUseCase.getAlertsBySeverity(severity);
        return ResponseEntity.ok(alerts.stream().map(this::toResponse).collect(Collectors.toList()));
    }

    @GetMapping("/{alertId}")
    @Operation(summary = "Get alert by ID")
    public ResponseEntity<AlertResponse> getAlertById(@PathVariable Long alertId) {
        return getAlertsUseCase.getAlertById(alertId)
                .map(alert -> ResponseEntity.ok(toResponse(alert)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{alertId}/acknowledge")
    @Operation(summary = "Acknowledge an alert")
    @Audited(action = AuditAction.ALERT_UPDATE, entityType = "Alert", description = "Acknowledging alert {alertId}")
    public ResponseEntity<AlertResponse> acknowledgeAlert(
            @PathVariable Long alertId,
            @Valid @RequestBody AlertOperationRequest request) {
        return manageAlertUseCase.acknowledgeAlert(alertId, request.getUserId())
                .map(alert -> ResponseEntity.ok(toResponse(alert)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{alertId}/resolve")
    @Operation(summary = "Resolve an alert")
    @Audited(action = AuditAction.ALERT_UPDATE, entityType = "Alert", description = "Resolving alert {alertId}")
    public ResponseEntity<AlertResponse> resolveAlert(
            @PathVariable Long alertId,
            @Valid @RequestBody AlertOperationRequest request) {
        return manageAlertUseCase.resolveAlert(alertId, request.getUserId())
                .map(alert -> ResponseEntity.ok(toResponse(alert)))
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{alertId}")
    @Operation(summary = "Delete an alert")
    @Audited(action = AuditAction.ALERT_DELETE, entityType = "Alert", description = "Deleting alert {alertId}")
    public ResponseEntity<Void> deleteAlert(@PathVariable Long alertId) {
        if (manageAlertUseCase.deleteAlert(alertId)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    private AlertResponse toResponse(Alert alert) {
        return new AlertResponse(
                alert.getId(),
                alert.getCustomerId(),
                alert.getResourceId(),
                alert.getMetricType(),
                alert.getSeverity(),
                alert.getStatus(),
                alert.getCurrentValue(),
                alert.getThresholdValue(),
                alert.getThresholdType(),
                alert.getMessage(),
                alert.getTriggeredAt(),
                alert.getResolvedAt(),
                alert.getAcknowledgedBy(),
                alert.getAcknowledgedAt(),
                alert.getResolvedBy(),
                alert.getSource()
        );
    }
}
