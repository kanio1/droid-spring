package com.droid.bss.infrastructure.database.repository;

import com.droid.bss.domain.partner.PartnerEntity;
import com.droid.bss.domain.partner.PartnerStatus;
import com.droid.bss.domain.partner.PartnerType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository for partners
 */
@Repository
public interface PartnerRepository extends JpaRepository<PartnerEntity, String> {

    /**
     * Find partner by code
     */
    Optional<PartnerEntity> findByPartnerCode(String partnerCode);

    /**
     * Find partners by type
     */
    List<PartnerEntity> findByPartnerType(PartnerType partnerType);

    /**
     * Find partners by status
     */
    List<PartnerEntity> findByStatus(PartnerStatus status);

    /**
     * Find partners by territory
     */
    List<PartnerEntity> findByTerritoryContaining(String territory);

    /**
     * Find partners with expiring contracts
     */
    @Query("SELECT p FROM PartnerEntity p WHERE p.contractEndDate BETWEEN :startDate AND :endDate")
    List<PartnerEntity> findContractsExpiringBetween(@Param("startDate") LocalDate startDate,
                                                     @Param("endDate") LocalDate endDate);

    /**
     * Find top partners by total sales
     */
    @Query("SELECT p FROM PartnerEntity p WHERE p.status = 'ACTIVE' ORDER BY p.totalSales DESC")
    List<PartnerEntity> findTopPartnersBySales(@Param("limit") int limit);

    /**
     * Find partners by commission rate range
     */
    List<PartnerEntity> findByCommissionRateBetween(BigDecimal minRate, BigDecimal maxRate);

    /**
     * Get total partners count
     */
    @Query("SELECT COUNT(p) FROM PartnerEntity p WHERE p.status = :status")
    long countByStatus(@Param("status") PartnerStatus status);

    /**
     * Get total commission by partner
     */
    @Query("SELECT COALESCE(SUM(p.totalCommission), 0) FROM PartnerEntity p WHERE p.id = :partnerId")
    BigDecimal getTotalCommissionByPartnerId(@Param("partnerId") String partnerId);

    /**
     * Get partners with outstanding balance
     */
    @Query("SELECT p FROM PartnerEntity p WHERE p.currentBalance > 0")
    List<PartnerEntity> findPartnersWithOutstandingBalance();

    /**
     * Find partners by market segment
     */
    List<PartnerEntity> findByMarketSegment(String marketSegment);

    /**
     * Search partners by name or code
     */
    @Query("SELECT p FROM PartnerEntity p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(p.partnerCode) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<PartnerEntity> searchPartners(@Param("searchTerm") String searchTerm);
}
