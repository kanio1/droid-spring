package com.droid.bss.domain.billing;

import com.droid.bss.domain.customer.CustomerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository for BillingCycleEntity
 */
@Repository
public interface BillingCycleRepository extends JpaRepository<BillingCycleEntity, String> {

    /**
     * Find billing cycles by customer
     */
    @Query("SELECT bc FROM BillingCycleEntity bc WHERE bc.customer = :customer ORDER BY bc.cycleStart DESC")
    List<BillingCycleEntity> findByCustomer(@Param("customer") CustomerEntity customer);

    /**
     * Find billing cycles by status
     */
    @Query("SELECT bc FROM BillingCycleEntity bc WHERE bc.status = :status ORDER BY bc.createdAt ASC")
    List<BillingCycleEntity> findByStatus(@Param("status") BillingCycleStatus status);

    /**
     * Find pending billing cycles
     */
    @Query("SELECT bc FROM BillingCycleEntity bc WHERE bc.status = 'PENDING' AND bc.billingDate <= :today ORDER BY bc.billingDate ASC")
    List<BillingCycleEntity> findPendingForProcessing(@Param("today") LocalDate today);

    /**
     * Find billing cycles by date range
     */
    @Query("SELECT bc FROM BillingCycleEntity bc WHERE bc.cycleStart <= :endDate AND bc.cycleEnd >= :startDate ORDER BY bc.cycleStart DESC")
    List<BillingCycleEntity> findByCycleOverlap(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * Count billing cycles by customer
     */
    @Query("SELECT COUNT(bc) FROM BillingCycleEntity bc WHERE bc.customer = :customer")
    Long countByCustomer(@Param("customer") CustomerEntity customer);

    /**
     * Find cycles with overlaps for same customer
     */
    @Query("SELECT bc1 FROM BillingCycleEntity bc1 WHERE EXISTS " +
           "(SELECT bc2 FROM BillingCycleEntity bc2 WHERE bc2.customer = bc1.customer " +
           "AND bc2.id != bc1.id AND bc2.cycleStart <= bc1.cycleEnd AND bc2.cycleEnd >= bc1.cycleStart)")
    List<BillingCycleEntity> findOverlappingCycles();
}
