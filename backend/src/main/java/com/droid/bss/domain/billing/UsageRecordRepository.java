package com.droid.bss.domain.billing;

import com.droid.bss.domain.subscription.SubscriptionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repository for UsageRecordEntity
 */
@Repository
public interface UsageRecordRepository extends JpaRepository<UsageRecordEntity, String> {

    /**
     * Find unrated usage records
     */
    @Query("SELECT u FROM UsageRecordEntity u WHERE u.rated = false ORDER BY u.usageDate ASC")
    List<UsageRecordEntity> findUnrated();

    /**
     * Find usage records by subscription
     */
    @Query("SELECT u FROM UsageRecordEntity u WHERE u.subscription = :subscription ORDER BY u.usageDate DESC")
    List<UsageRecordEntity> findBySubscription(@Param("subscription") SubscriptionEntity subscription);

    /**
     * Find usage records by date range
     */
    @Query("SELECT u FROM UsageRecordEntity u WHERE u.usageDate BETWEEN :startDate AND :endDate ORDER BY u.usageDate DESC")
    List<UsageRecordEntity> findByDateRange(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * Find unprocessed usage records
     */
    @Query("SELECT u FROM UsageRecordEntity u WHERE u.processed = false")
    List<UsageRecordEntity> findUnprocessed();

    /**
     * Count unrated records
     */
    @Query("SELECT COUNT(u) FROM UsageRecordEntity u WHERE u.rated = false")
    Long countUnrated();

    /**
     * Find by subscription and date range
     */
    @Query("SELECT u FROM UsageRecordEntity u WHERE u.subscription = :subscription " +
           "AND u.usageDate BETWEEN :startDate AND :endDate " +
           "ORDER BY u.usageDate DESC")
    List<UsageRecordEntity> findBySubscriptionAndDateRange(
            @Param("subscription") SubscriptionEntity subscription,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
