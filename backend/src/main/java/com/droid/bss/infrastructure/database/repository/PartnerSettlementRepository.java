package com.droid.bss.infrastructure.database.repository;

import com.droid.bss.domain.partner.PartnerSettlement;
import com.droid.bss.domain.partner.SettlementStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository for partner settlements
 */
@Repository
public interface PartnerSettlementRepository extends JpaRepository<PartnerSettlement, Long> {

    /**
     * Find settlements by partner ID
     */
    List<PartnerSettlement> findByPartnerIdOrderBySettlementDateDesc(String partnerId);

    /**
     * Find settlements by status
     */
    List<PartnerSettlement> findByStatus(SettlementStatus status);

    /**
     * Find settlements by period
     */
    List<PartnerSettlement> findByPeriodStartAndPeriodEnd(LocalDate periodStart, LocalDate periodEnd);

    /**
     * Find pending settlements
     */
    @Query("SELECT s FROM PartnerSettlement s WHERE s.status = 'PENDING'")
    List<PartnerSettlement> findPendingSettlements();

    /**
     * Get total commission for a partner in a period
     */
    @Query("SELECT COALESCE(SUM(s.netCommission), 0) FROM PartnerSettlement s " +
           "WHERE s.partner.id = :partnerId AND s.periodStart >= :startDate AND s.periodEnd <= :endDate")
    BigDecimal getTotalCommissionForPartner(@Param("partnerId") String partnerId,
                                           @Param("startDate") LocalDate startDate,
                                           @Param("endDate") LocalDate endDate);

    /**
     * Count settlements by status
     */
    @Query("SELECT COUNT(s) FROM PartnerSettlement s WHERE s.status = :status")
    long countByStatus(@Param("status") SettlementStatus status);
}
