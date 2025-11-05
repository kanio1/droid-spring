package com.droid.bss.domain.asset;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository for SIMCard entities
 */
@Repository
public interface SIMCardRepository extends JpaRepository<SIMCardEntity, String> {

    /**
     * Find SIM cards by status
     */
    List<SIMCardEntity> findByStatus(SIMCardStatus status);

    /**
     * Find SIM card by ICCID
     */
    Optional<SIMCardEntity> findByIccidAndDeletedAtIsNull(String iccid);

    /**
     * Find SIM card by IMSI
     */
    @Query("SELECT s FROM SIMCardEntity s WHERE s.imsi = :imsi AND s.deletedAt IS NULL")
    Optional<SIMCardEntity> findByImsiAndDeletedAtIsNull(@Param("imsi") String imsi);

    /**
     * Find SIM card by MSISDN
     */
    @Query("SELECT s FROM SIMCardEntity s WHERE s.msisdn = :msisdn AND s.deletedAt IS NULL")
    Optional<SIMCardEntity> findByMsisdnAndDeletedAtIsNull(@Param("msisdn") String msisdn);

    /**
     * Find available SIM cards
     */
    @Query("SELECT s FROM SIMCardEntity s WHERE s.status = 'AVAILABLE' AND s.deletedAt IS NULL")
    List<SIMCardEntity> findAvailable();

    /**
     * Find SIM cards assigned to customer
     */
    @Query("SELECT s FROM SIMCardEntity s WHERE s.assignedToId = :customerId AND s.deletedAt IS NULL")
    List<SIMCardEntity> findByCustomerId(@Param("customerId") String customerId);

    /**
     * Find expired SIM cards
     */
    @Query("SELECT s FROM SIMCardEntity s WHERE s.expiryDate < :currentDate AND s.deletedAt IS NULL")
    List<SIMCardEntity> findExpired(@Param("currentDate") LocalDate currentDate);

    /**
     * Find SIM cards expiring soon (in next N days)
     */
    @Query("SELECT s FROM SIMCardEntity s WHERE s.expiryDate BETWEEN :startDate AND :endDate AND s.deletedAt IS NULL")
    List<SIMCardEntity> findExpiringBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * Find SIM cards with data limits
     */
    @Query("SELECT s FROM SIMCardEntity s WHERE s.dataLimitMb IS NOT NULL AND s.deletedAt IS NULL")
    List<SIMCardEntity> findWithDataLimits();

    /**
     * Find SIM cards with voice limits
     */
    @Query("SELECT s FROM SIMCardEntity s WHERE s.voiceLimitMinutes IS NOT NULL AND s.deletedAt IS NULL")
    List<SIMCardEntity> findWithVoiceLimits();

    /**
     * Find SIM cards with SMS limits
     */
    @Query("SELECT s FROM SIMCardEntity s WHERE s.smsLimit IS NOT NULL AND s.deletedAt IS NULL")
    List<SIMCardEntity> findWithSmsLimits();

    /**
     * Count SIM cards by status
     */
    @Query("SELECT s.status, COUNT(s) FROM SIMCardEntity s WHERE s.deletedAt IS NULL GROUP BY s.status")
    List<Object[]> countByStatus();

    /**
     * Find suspended SIM cards
     */
    @Query("SELECT s FROM SIMCardEntity s WHERE s.status = 'SUSPENDED' AND s.deletedAt IS NULL")
    List<SIMCardEntity> findSuspended();

    /**
     * Find damaged SIM cards
     */
    @Query("SELECT s FROM SIMCardEntity s WHERE s.status = 'DAMAGED' AND s.deletedAt IS NULL")
    List<SIMCardEntity> findDamaged();
}
