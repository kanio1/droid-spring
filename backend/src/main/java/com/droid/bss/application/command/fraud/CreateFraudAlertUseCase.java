package com.droid.bss.application.command.fraud;

import com.droid.bss.domain.fraud.FraudAlertEntity;
import com.droid.bss.domain.fraud.FraudAlertType;
import com.droid.bss.infrastructure.database.repository.FraudAlertRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Use case for creating fraud alerts
 */
@Service
@Transactional
public class CreateFraudAlertUseCase {

    private final FraudAlertRepository fraudAlertRepository;

    public CreateFraudAlertUseCase(FraudAlertRepository fraudAlertRepository) {
        this.fraudAlertRepository = fraudAlertRepository;
    }

    public FraudAlertEntity handle(CreateFraudAlertCommand command) {
        // Generate alert ID
        String alertId = command.alertId() != null ?
                command.alertId() : generateAlertId();

        // Check if alert ID already exists
        if (fraudAlertRepository.findByAlertId(alertId) != null) {
            throw new IllegalArgumentException("Alert ID already exists: " + alertId);
        }

        // Create alert
        FraudAlertEntity alert = new FraudAlertEntity(
                alertId,
                command.customerId(),
                command.alertType(),
                command.title(),
                command.description()
        );
        alert.setAccountId(command.accountId());
        alert.setSeverity(command.severity());
        alert.setRuleTriggered(command.ruleTriggered());
        alert.setRiskScore(command.riskScore());
        alert.setSourceEntityType(command.sourceEntityType());
        alert.setSourceEntityId(command.sourceEntityId());
        alert.setTransactionId(command.transactionId());
        alert.setIpAddress(command.ipAddress());
        alert.setUserAgent(command.userAgent());
        alert.setLocation(command.location());
        alert.setDetails(command.details());
        alert.setSourceSystem(command.sourceSystem());

        return fraudAlertRepository.save(alert);
    }

    public FraudAlertEntity handleVelocityCheck(
            String customerId,
            String entityType,
            String entityId,
            int transactionCount,
            int timeWindowMinutes,
            String ipAddress
    ) {
        java.math.BigDecimal riskScore = calculateVelocityRiskScore(transactionCount, timeWindowMinutes);

        FraudAlertEntity alert = handle(new CreateFraudAlertCommand(
                null,
                customerId,
                entityType,
                entityId,
                null,
                FraudAlertType.VELOCITY_CHECK,
                "Velocity Check Violation",
                "Customer performed " + transactionCount + " transactions in " + timeWindowMinutes + " minutes",
                riskScore.compareTo(new java.math.BigDecimal("60")) >= 0 ? "HIGH" : "MEDIUM",
                "VELOCITY_RULE",
                riskScore,
                entityType,
                entityId,
                null,
                ipAddress,
                null,
                null,
                "Detected unusual transaction velocity",
                "SYSTEM"
        ));

        return alert;
    }

    public FraudAlertEntity handleHighValueTransaction(
            String customerId,
            String transactionId,
            java.math.BigDecimal amount,
            String location
    ) {
        java.math.BigDecimal riskScore = calculateHighValueRiskScore(amount);

        FraudAlertEntity alert = handle(new CreateFraudAlertCommand(
                null,
                customerId,
                null,
                null,
                null,
                FraudAlertType.HIGH_VALUE_TRANSACTION,
                "High Value Transaction",
                "Transaction amount: " + amount + " exceeds normal thresholds",
                riskScore.compareTo(new java.math.BigDecimal("70")) >= 0 ? "HIGH" : "MEDIUM",
                "HIGH_VALUE_RULE",
                riskScore,
                "TRANSACTION",
                transactionId,
                transactionId,
                null,
                null,
                location,
                "Transaction details exceed normal parameters",
                "SYSTEM"
        ));

        return alert;
    }

    private java.math.BigDecimal calculateVelocityRiskScore(int transactionCount, int timeWindowMinutes) {
        // Simple risk calculation
        if (transactionCount > 20 && timeWindowMinutes < 60) {
            return new java.math.BigDecimal("85");
        } else if (transactionCount > 10 && timeWindowMinutes < 60) {
            return new java.math.BigDecimal("70");
        } else if (transactionCount > 5 && timeWindowMinutes < 60) {
            return new java.math.BigDecimal("50");
        } else {
            return new java.math.BigDecimal("30");
        }
    }

    private java.math.BigDecimal calculateHighValueRiskScore(java.math.BigDecimal amount) {
        // Simple risk calculation based on amount
        if (amount.compareTo(new java.math.BigDecimal("10000")) >= 0) {
            return new java.math.BigDecimal("90");
        } else if (amount.compareTo(new java.math.BigDecimal("5000")) >= 0) {
            return new java.math.BigDecimal("75");
        } else if (amount.compareTo(new java.math.BigDecimal("1000")) >= 0) {
            return new java.math.BigDecimal("60");
        } else {
            return new java.math.BigDecimal("40");
        }
    }

    private String generateAlertId() {
        return "FA-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
    }

    // Command DTO
    public record CreateFraudAlertCommand(
            String alertId,
            String customerId,
            String accountId,
            String sourceEntityType,
            String sourceEntityId,
            FraudAlertType alertType,
            String title,
            String description,
            String severity,
            String ruleTriggered,
            java.math.BigDecimal riskScore,
            String sourceEntity,
            String sourceEntityId1,
            String transactionId,
            String ipAddress,
            String userAgent,
            String location,
            String details,
            String sourceSystem
    ) {}
}
