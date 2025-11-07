package com.droid.bss.infrastructure.database.repository;

import com.droid.bss.domain.fraud.FraudAlertEntity;
import com.droid.bss.domain.fraud.FraudAlertStatus;
import com.droid.bss.domain.fraud.FraudAlertType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for fraud alerts
 */
@Repository
public interface FraudAlertRepository extends JpaRepository<FraudAlertEntity, String> {

    /**
     * Find by alert ID
     */
    FraudAlertEntity findByAlertId(String alertId);

    /**
     * Find by customer ID
     */
    List<FraudAlertEntity> findByCustomerId(String customerId);

    /**
     * Find by status
     */
    List<FraudAlertEntity> findByStatus(FraudAlertStatus status);

    /**
     * Find by alert type
     */
    List<FraudAlertEntity> findByAlertType(FraudAlertType alertType);

    /**
     * Find open alerts
     */
    @Query("SELECT f FROM FraudAlertEntity f WHERE f.status IN ('NEW', 'ASSIGNED', 'IN_REVIEW', 'ESCALATED')")
    List<FraudAlertEntity> findOpenAlerts();

    /**
     * Find high risk alerts
     */
    @Query("SELECT f FROM FraudAlertEntity f WHERE f.riskScore >= 60 AND f.status IN ('NEW', 'ASSIGNED', 'IN_REVIEW')")
    List<FraudAlertEntity> findHighRiskAlerts();

    /**
     * Find by severity
     */
    @Query("SELECT f FROM FraudAlertEntity f WHERE f.severity = :severity AND f.status != 'CLOSED'")
    List<FraudAlertEntity> findBySeverity(@Param("severity") String severity);

    /**
     * Find alerts in date range
     */
    @Query("SELECT f FROM FraudAlertEntity f WHERE f.createdAt BETWEEN :startDate AND :endDate")
    List<FraudAlertEntity> findByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    /**
     * Find by transaction ID
     */
    FraudAlertEntity findByTransactionId(String transactionId);

    /**
     * Find by IP address
     */
    @Query("SELECT f FROM FraudAlertEntity f WHERE f.ipAddress = :ipAddress AND f.createdAt > :since")
    List<FraudAlertEntity> findByIpAddress(@Param("ipAddress") String ipAddress, @Param("since") LocalDateTime since);

    /**
     * Count alerts by status
     */
    @Query("SELECT COUNT(f) FROM FraudAlertEntity f WHERE f.status = :status")
    long countByStatus(@Param("status") FraudAlertStatus status);

    /**
     * Count open alerts
     */
    @Query("SELECT COUNT(f) FROM FraudAlertEntity f WHERE f.status IN ('NEW', 'ASSIGNED', 'IN_REVIEW', 'ESCALATED')")
    long countOpenAlerts();

    /**
     * Get average resolution time
     */
    @Query("SELECT AVG(EXTRACT(EPOCH FROM (f.resolvedAt - f.createdAt))/3600) FROM FraudAlertEntity f WHERE f.resolvedAt IS NOT NULL")
    Double getAverageResolutionHours();

    /**
     * Find false positives
     */
    @Query("SELECT f FROM FraudAlertEntity f WHERE f.falsePositive = true")
    List<FraudAlertEntity> findFalsePositives();

    /**
     * Find alerts by rule
     */
    @Query("SELECT f FROM FraudAlertEntity f WHERE f.ruleTriggered = :ruleCode")
    List<FraudAlertEntity> findByRule(@Param("ruleCode") String ruleCode);

    /**
     * Find alerts assigned to analyst
     */
    List<FraudAlertEntity> findByAssignedTo(String assignedTo);
}
