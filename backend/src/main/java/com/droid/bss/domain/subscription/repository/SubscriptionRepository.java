package com.droid.bss.domain.subscription.repository;

import com.droid.bss.domain.subscription.SubscriptionEntity;
import com.droid.bss.domain.subscription.SubscriptionStatus;
import com.droid.bss.domain.customer.CustomerEntity;
import com.droid.bss.domain.product.ProductEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for SubscriptionEntity
 */
@Repository
public interface SubscriptionRepository extends JpaRepository<SubscriptionEntity, UUID> {

    /**
     * Find subscription by subscription number
     */
    Optional<SubscriptionEntity> findBySubscriptionNumber(String subscriptionNumber);

    /**
     * Find subscriptions by customer
     */
    Page<SubscriptionEntity> findByCustomer(CustomerEntity customer, Pageable pageable);

    /**
     * Find subscriptions by customer ID
     */
    @Query("SELECT s FROM SubscriptionEntity s WHERE s.customer.id = :customerId")
    Page<SubscriptionEntity> findByCustomerId(@Param("customerId") UUID customerId, Pageable pageable);

    /**
     * Find subscriptions by product
     */
    Page<SubscriptionEntity> findByProduct(ProductEntity product, Pageable pageable);

    /**
     * Find subscriptions by status
     */
    Page<SubscriptionEntity> findByStatus(SubscriptionStatus status, Pageable pageable);

    /**
     * Find active subscriptions
     */
    @Query("SELECT s FROM SubscriptionEntity s WHERE s.status = :status")
    Page<SubscriptionEntity> findActiveSubscriptions(@Param("status") SubscriptionStatus status, Pageable pageable);

    /**
     * Find subscriptions by billing period
     */
    @Query("SELECT s FROM SubscriptionEntity s WHERE s.billingPeriod = :billingPeriod")
    Page<SubscriptionEntity> findByBillingPeriod(@Param("billingPeriod") String billingPeriod, Pageable pageable);

    /**
     * Find subscriptions expiring within given days
     */
    @Query("SELECT s FROM SubscriptionEntity s WHERE s.endDate IS NOT NULL " +
           "AND s.endDate BETWEEN CURRENT_DATE AND :endDate")
    List<SubscriptionEntity> findExpiringSubscriptions(@Param("endDate") LocalDate endDate);

    /**
     * Find subscriptions needing renewal (next billing date reached)
     */
    @Query("SELECT s FROM SubscriptionEntity s WHERE s.nextBillingDate = CURRENT_DATE " +
           "AND s.autoRenew = true")
    List<SubscriptionEntity> findSubscriptionsForRenewal();

    /**
     * Find subscriptions by product ID
     */
    @Query("SELECT s FROM SubscriptionEntity s WHERE s.product.id = :productId")
    Page<SubscriptionEntity> findByProductId(@Param("productId") UUID productId, Pageable pageable);

    /**
     * Count subscriptions by customer
     */
    long countByCustomer(CustomerEntity customer);

    /**
     * Count subscriptions by status
     */
    long countByStatus(SubscriptionStatus status);

    /**
     * Check if subscription number exists
     */
    boolean existsBySubscriptionNumber(String subscriptionNumber);

    /**
     * Find subscriptions with auto-renewal enabled
     */
    @Query("SELECT s FROM SubscriptionEntity s WHERE s.autoRenew = true")
    Page<SubscriptionEntity> findAutoRenewSubscriptions(Pageable pageable);

    /**
     * Find suspended subscriptions
     */
    @Query("SELECT s FROM SubscriptionEntity s WHERE s.status = :status")
    Page<SubscriptionEntity> findSuspendedSubscriptions(@Param("status") SubscriptionStatus status, Pageable pageable);

    /**
     * Find subscriptions by date range
     */
    @Query("SELECT s FROM SubscriptionEntity s WHERE s.startDate BETWEEN :startDate AND :endDate")
    Page<SubscriptionEntity> findByStartDateRange(@Param("startDate") LocalDate startDate,
                                                 @Param("endDate") LocalDate endDate,
                                                 Pageable pageable);

    /**
     * Search subscriptions by subscription number
     */
    @Query("SELECT s FROM SubscriptionEntity s WHERE " +
           "LOWER(s.subscriptionNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<SubscriptionEntity> searchSubscriptions(@Param("searchTerm") String searchTerm, Pageable pageable);
}
