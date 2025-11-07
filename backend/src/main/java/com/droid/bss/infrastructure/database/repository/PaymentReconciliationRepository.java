package com.droid.bss.infrastructure.database.repository;

import com.droid.bss.domain.payment.PaymentReconciliationEntity;
import com.droid.bss.domain.payment.ReconciliationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository for payment reconciliation
 */
@Repository
public interface PaymentReconciliationRepository extends JpaRepository<PaymentReconciliationEntity, String> {

    /**
     * Find reconciliation by ID
     */
    Optional<PaymentReconciliationEntity> findByReconciliationId(String reconciliationId);

    /**
     * Find reconciliations by period
     */
    List<PaymentReconciliationEntity> findByPeriodStartBetween(LocalDate startDate, LocalDate endDate);

    /**
     * Find reconciliations by status
     */
    List<PaymentReconciliationEntity> findByStatus(ReconciliationStatus status);

    /**
     * Find reconciliations by gateway
     */
    List<PaymentReconciliationEntity> findByGatewayName(String gatewayName);

    /**
     * Find pending reconciliations
     */
    @Query("SELECT r FROM PaymentReconciliationEntity r WHERE r.status = 'PENDING'")
    List<PaymentReconciliationEntity> findPendingReconciliations();

    /**
     * Find reconciliations with discrepancies
     */
    @Query("SELECT r FROM PaymentReconciliationEntity r WHERE r.discrepancyCount > 0")
    List<PaymentReconciliationEntity> findReconciliationsWithDiscrepancies();

    /**
     * Get total matched amount for a period
     */
    @Query("SELECT COALESCE(SUM(r.matchedAmount), 0) FROM PaymentReconciliationEntity r " +
           "WHERE r.periodStart >= :startDate AND r.periodEnd <= :endDate")
    java.math.BigDecimal getTotalMatchedAmount(@Param("startDate") LocalDate startDate,
                                               @Param("endDate") LocalDate endDate);

    /**
     * Count reconciliations by status
     */
    @Query("SELECT COUNT(r) FROM PaymentReconciliationEntity r WHERE r.status = :status")
    long countByStatus(@Param("status") ReconciliationStatus status);
}
