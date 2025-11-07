package com.droid.bss.api.monitoring;

import com.droid.bss.application.dto.monitoring.NotificationPreferenceRequest;
import com.droid.bss.application.dto.monitoring.NotificationPreferenceResponse;
import com.droid.bss.application.service.monitoring.ManageNotificationPreferenceUseCase;
import com.droid.bss.domain.audit.AuditAction;
import com.droid.bss.infrastructure.audit.Audited;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST API controller for notification preferences
 */
@RestController
@RequestMapping("/api/v1/notification-preferences")
@Tag(name = "Notification Preferences", description = "Notification preferences management API")
public class NotificationPreferencesController {

    private final ManageNotificationPreferenceUseCase useCase;

    public NotificationPreferencesController(ManageNotificationPreferenceUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping("/{customerId}")
    @Operation(summary = "Get notification preference by customer ID")
    public ResponseEntity<NotificationPreferenceResponse> getPreferenceByCustomerId(
            @PathVariable Long customerId) {
        return useCase.getPreferenceByCustomerId(customerId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Create notification preference")
    @Audited(action = AuditAction.MONITORING_CREATE, entityType = "NotificationPreference", description = "Creating notification preference")
    public ResponseEntity<NotificationPreferenceResponse> createPreference(
            @Valid @RequestBody NotificationPreferenceRequest request) {
        NotificationPreferenceResponse response = useCase.createPreference(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update notification preference")
    @Audited(action = AuditAction.MONITORING_UPDATE, entityType = "NotificationPreference", description = "Updating notification preference {id}")
    public ResponseEntity<NotificationPreferenceResponse> updatePreference(
            @PathVariable Long id,
            @Valid @RequestBody NotificationPreferenceRequest request) {
        return useCase.updatePreference(id, request)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete notification preference")
    @Audited(action = AuditAction.MONITORING_DELETE, entityType = "NotificationPreference", description = "Deleting notification preference {id}")
    public ResponseEntity<Void> deletePreference(@PathVariable Long id) {
        if (useCase.deletePreference(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
