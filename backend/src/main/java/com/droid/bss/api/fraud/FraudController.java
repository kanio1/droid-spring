package com.droid.bss.api.fraud;

import com.droid.bss.application.command.fraud.CreateFraudAlertUseCase;
import com.droid.bss.domain.audit.AuditAction;
import com.droid.bss.domain.fraud.*;
import com.droid.bss.infrastructure.audit.Audited;
import com.droid.bss.infrastructure.database.repository.FraudAlertRepository;
import com.droid.bss.infrastructure.database.repository.FraudRuleRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST API for fraud management
 */
@RestController
@RequestMapping("/api/fraud")
public class FraudController {

    private final CreateFraudAlertUseCase createFraudAlertUseCase;
    private final FraudAlertRepository fraudAlertRepository;
    private final FraudRuleRepository fraudRuleRepository;

    public FraudController(
            CreateFraudAlertUseCase createFraudAlertUseCase,
            FraudAlertRepository fraudAlertRepository,
            FraudRuleRepository fraudRuleRepository
    ) {
        this.createFraudAlertUseCase = createFraudAlertUseCase;
        this.fraudAlertRepository = fraudAlertRepository;
        this.fraudRuleRepository = fraudRuleRepository;
    }

    // Fraud Alert endpoints
    @GetMapping("/alerts")
    public ResponseEntity<List<FraudAlertEntity>> getAllAlerts(
            @RequestParam(required = false) FraudAlertStatus status,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String customerId
    ) {
        List<FraudAlertEntity> alerts;
        if (status != null) {
            alerts = fraudAlertRepository.findByStatus(status);
        } else if (severity != null) {
            alerts = fraudAlertRepository.findBySeverity(severity);
        } else if (customerId != null) {
            alerts = fraudAlertRepository.findByCustomerId(customerId);
        } else {
            alerts = fraudAlertRepository.findAll();
        }
        return ResponseEntity.ok(alerts);
    }

    @GetMapping("/alerts/open")
    public ResponseEntity<List<FraudAlertEntity>> getOpenAlerts() {
        List<FraudAlertEntity> alerts = fraudAlertRepository.findOpenAlerts();
        return ResponseEntity.ok(alerts);
    }

    @GetMapping("/alerts/high-risk")
    public ResponseEntity<List<FraudAlertEntity>> getHighRiskAlerts() {
        List<FraudAlertEntity> alerts = fraudAlertRepository.findHighRiskAlerts();
        return ResponseEntity.ok(alerts);
    }

    @GetMapping("/alerts/{id}")
    public ResponseEntity<FraudAlertEntity> getAlertById(@PathVariable String id) {
        FraudAlertEntity alert = fraudAlertRepository.findByAlertId(id);
        return alert != null ? ResponseEntity.ok(alert) : ResponseEntity.notFound().build();
    }

    @PostMapping("/alerts")
    @Audited(action = AuditAction.FRAUD_CREATE, entityType = "FraudAlert", description = "Creating fraud alert")
    public ResponseEntity<FraudAlertEntity> createAlert(@RequestBody CreateFraudAlertUseCase.CreateFraudAlertCommand command) {
        FraudAlertEntity alert = createFraudAlertUseCase.handle(command);
        return ResponseEntity.ok(alert);
    }

    @PostMapping("/alerts/{id}/assign")
    @Audited(action = AuditAction.FRAUD_UPDATE, entityType = "FraudAlert", description = "Assigning fraud alert {id}")
    public ResponseEntity<FraudAlertEntity> assignAlert(
            @PathVariable String id,
            @RequestBody Map<String, String> request
    ) {
        String analystId = request.get("analystId");
        FraudAlertEntity alert = fraudAlertRepository.findByAlertId(id);
        if (alert != null) {
            alert.assignTo(analystId);
            fraudAlertRepository.save(alert);
            return ResponseEntity.ok(alert);
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/alerts/{id}/resolve")
    @Audited(action = AuditAction.FRAUD_UPDATE, entityType = "FraudAlert", description = "Resolving fraud alert {id}")
    public ResponseEntity<FraudAlertEntity> resolveAlert(
            @PathVariable String id,
            @RequestBody Map<String, String> request
    ) {
        String resolvedBy = request.get("resolvedBy");
        String resolutionNotes = request.get("resolutionNotes");
        FraudAlertEntity alert = fraudAlertRepository.findByAlertId(id);
        if (alert != null) {
            alert.resolve(resolvedBy, resolutionNotes);
            fraudAlertRepository.save(alert);
            return ResponseEntity.ok(alert);
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/alerts/{id}/false-positive")
    @Audited(action = AuditAction.FRAUD_UPDATE, entityType = "FraudAlert", description = "Marking fraud alert {id} as false positive")
    public ResponseEntity<FraudAlertEntity> markAsFalsePositive(
            @PathVariable String id,
            @RequestBody Map<String, String> request
    ) {
        String notedBy = request.get("notedBy");
        String reason = request.get("reason");
        FraudAlertEntity alert = fraudAlertRepository.findByAlertId(id);
        if (alert != null) {
            alert.markAsFalsePositive(notedBy, reason);
            fraudAlertRepository.save(alert);
            return ResponseEntity.ok(alert);
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/alerts/{id}/escalate")
    @Audited(action = AuditAction.FRAUD_UPDATE, entityType = "FraudAlert", description = "Escalating fraud alert {id}")
    public ResponseEntity<FraudAlertEntity> escalateAlert(
            @PathVariable String id,
            @RequestBody Map<String, String> request
    ) {
        String escalatedTo = request.get("escalatedTo");
        FraudAlertEntity alert = fraudAlertRepository.findByAlertId(id);
        if (alert != null) {
            alert.escalate(escalatedTo);
            fraudAlertRepository.save(alert);
            return ResponseEntity.ok(alert);
        }
        return ResponseEntity.notFound().build();
    }

    // Convenience endpoints for specific fraud checks
    @PostMapping("/checks/velocity")
    @Audited(action = AuditAction.FRAUD_CREATE, entityType = "FraudAlert", description = "Performing velocity check fraud analysis")
    public ResponseEntity<FraudAlertEntity> checkVelocity(
            @RequestBody Map<String, Object> request
    ) {
        String customerId = (String) request.get("customerId");
        String entityType = (String) request.get("entityType");
        String entityId = (String) request.get("entityId");
        int transactionCount = (Integer) request.get("transactionCount");
        int timeWindowMinutes = (Integer) request.get("timeWindowMinutes");
        String ipAddress = (String) request.get("ipAddress");

        FraudAlertEntity alert = createFraudAlertUseCase.handleVelocityCheck(
                customerId, entityType, entityId, transactionCount, timeWindowMinutes, ipAddress
        );
        return ResponseEntity.ok(alert);
    }

    @PostMapping("/checks/high-value")
    @Audited(action = AuditAction.FRAUD_CREATE, entityType = "FraudAlert", description = "Performing high-value transaction fraud analysis")
    public ResponseEntity<FraudAlertEntity> checkHighValue(
            @RequestBody Map<String, Object> request
    ) {
        String customerId = (String) request.get("customerId");
        String transactionId = (String) request.get("transactionId");
        BigDecimal amount = new BigDecimal(request.get("amount").toString());
        String location = (String) request.get("location");

        FraudAlertEntity alert = createFraudAlertUseCase.handleHighValueTransaction(
                customerId, transactionId, amount, location
        );
        return ResponseEntity.ok(alert);
    }

    // Statistics endpoint
    @GetMapping("/statistics")
    public ResponseEntity<FraudStatistics> getFraudStatistics() {
        long totalAlerts = fraudAlertRepository.count();
        long openAlerts = fraudAlertRepository.countOpenAlerts();
        long highRiskAlerts = fraudAlertRepository.findHighRiskAlerts().size();
        long resolvedToday = fraudAlertRepository.findByDateRange(
                LocalDateTime.now().toLocalDate().atStartOfDay(),
                LocalDateTime.now()
        ).size();
        Double avgResolutionTime = fraudAlertRepository.getAverageResolutionHours();
        long falsePositives = fraudAlertRepository.findFalsePositives().size();

        return ResponseEntity.ok(new FraudStatistics(
                totalAlerts,
                openAlerts,
                highRiskAlerts,
                resolvedToday,
                avgResolutionTime != null ? avgResolutionTime : 0.0,
                falsePositives
        ));
    }

    // DTOs
    public record FraudStatistics(
            long totalAlerts,
            long openAlerts,
            long highRiskAlerts,
            long resolvedToday,
            double avgResolutionHours,
            long falsePositives
    ) {}
}
