package com.droid.bss.domain.subscription;

import com.droid.bss.domain.customer.CustomerId;
import com.droid.bss.domain.order.OrderId;
import com.droid.bss.domain.product.ProductId;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * SubscriptionRepository - DDD Port for Subscription Aggregate
 * This is the interface (port) that the domain depends on.
 * The implementation will be in the infrastructure layer.
 */
public interface SubscriptionRepository {

    /**
     * Find subscription by ID
     */
    Optional<Subscription> findById(SubscriptionId id);

    /**
     * Find subscription by subscription number
     */
    Optional<Subscription> findBySubscriptionNumber(String subscriptionNumber);

    /**
     * Find subscriptions by customer
     */
    List<Subscription> findByCustomerId(CustomerId customerId);

    /**
     * Find subscriptions by product
     */
    List<Subscription> findByProductId(ProductId productId);

    /**
     * Find subscriptions by order
     */
    List<Subscription> findByOrderId(OrderId orderId);

    /**
     * Find subscriptions by status
     */
    List<Subscription> findByStatus(SubscriptionStatus status);

    /**
     * Find active subscriptions by customer
     */
    List<Subscription> findActiveByCustomerId(CustomerId customerId);

    /**
     * Find suspended subscriptions by customer
     */
    List<Subscription> findSuspendedByCustomerId(CustomerId customerId);

    /**
     * Find subscriptions due for renewal
     */
    List<Subscription> findDueForRenewal(LocalDate date);

    /**
     * Find expired subscriptions
     */
    List<Subscription> findExpired();

    /**
     * Find subscriptions with auto-renew enabled
     */
    List<Subscription> findByAutoRenewTrue();

    /**
     * Find subscriptions by billing period
     */
    List<Subscription> findByBillingPeriod(String billingPeriod);

    /**
     * Save subscription (create or update)
     */
    Subscription save(Subscription subscription);

    /**
     * Delete subscription by ID
     */
    void deleteById(SubscriptionId id);

    /**
     * Check if subscription exists by ID
     */
    boolean existsById(SubscriptionId id);

    /**
     * Check if subscription number exists
     */
    boolean existsBySubscriptionNumber(String subscriptionNumber);

    /**
     * Count subscriptions by customer
     */
    long countByCustomerId(CustomerId customerId);

    /**
     * Count active subscriptions by customer
     */
    long countActiveByCustomerId(CustomerId customerId);
}

