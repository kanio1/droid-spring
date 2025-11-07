package com.droid.bss.infrastructure.write;

import com.droid.bss.domain.subscription.*;
import com.droid.bss.domain.subscription.event.SubscriptionEventPublisher;
import com.droid.bss.domain.customer.CustomerId;
import com.droid.bss.domain.product.ProductId;
import com.droid.bss.domain.order.OrderId;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Repository
@Transactional
public class SubscriptionRepositoryImpl implements SubscriptionRepository {

    @PersistenceContext
    private EntityManager entityManager;

    private final SubscriptionEventPublisher eventPublisher;

    public SubscriptionRepositoryImpl(SubscriptionEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Subscription save(Subscription subscription) {
        SubscriptionEntity entity = SubscriptionEntity.from(subscription);

        // Check if this is a new entity
        Long count = entityManager.createQuery(
            "SELECT COUNT(s) FROM SubscriptionEntity s WHERE s.id = :id",
            Long.class
        ).setParameter("id", entity.getId())
         .getSingleResult();

        boolean exists = count != null && count > 0;

        // Save using merge
        SubscriptionEntity merged = entityManager.merge(entity);
        entityManager.flush();

        // Publish event (only for new subscriptions for now)
        if (!exists) {
            // Subscription was created
            try {
                eventPublisher.publishSubscriptionCreated(merged);
            } catch (Exception e) {
                // Log but don't fail
                System.err.println("Failed to publish event: " + e.getMessage());
            }
        }

        // Return as domain
        return merged.toDomain();
    }

    @Override
    public Optional<Subscription> findById(SubscriptionId id) {
        SubscriptionEntity entity = entityManager.find(SubscriptionEntity.class, id.value());
        return entity != null ? Optional.of(entity.toDomain()) : Optional.empty();
    }

    @Override
    public Optional<Subscription> findBySubscriptionNumber(String subscriptionNumber) {
        try {
            SubscriptionEntity entity = entityManager.createQuery(
                "SELECT s FROM SubscriptionEntity s WHERE s.subscriptionNumber = :subscriptionNumber",
                SubscriptionEntity.class
            ).setParameter("subscriptionNumber", subscriptionNumber)
             .getSingleResult();

            return Optional.of(entity.toDomain());
        } catch (jakarta.persistence.NoResultException e) {
            return Optional.empty();
        }
    }

    @Override
    public List<Subscription> findByCustomerId(CustomerId customerId) {
        List<SubscriptionEntity> entities = entityManager.createQuery(
            "SELECT s FROM SubscriptionEntity s WHERE s.customer.id = :customerId ORDER BY s.createdAt DESC",
            SubscriptionEntity.class
        ).setParameter("customerId", customerId.value())
         .getResultList();

        return entities.stream()
            .map(SubscriptionEntity::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Subscription> findByProductId(ProductId productId) {
        List<SubscriptionEntity> entities = entityManager.createQuery(
            "SELECT s FROM SubscriptionEntity s WHERE s.product.id = :productId ORDER BY s.createdAt DESC",
            SubscriptionEntity.class
        ).setParameter("productId", productId.value())
         .getResultList();

        return entities.stream()
            .map(SubscriptionEntity::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Subscription> findByOrderId(OrderId orderId) {
        List<SubscriptionEntity> entities = entityManager.createQuery(
            "SELECT s FROM SubscriptionEntity s WHERE s.order.id = :orderId ORDER BY s.createdAt DESC",
            SubscriptionEntity.class
        ).setParameter("orderId", orderId.value())
         .getResultList();

        return entities.stream()
            .map(SubscriptionEntity::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Subscription> findByStatus(SubscriptionStatus status) {
        List<SubscriptionEntity> entities = entityManager.createQuery(
            "SELECT s FROM SubscriptionEntity s WHERE s.status = :status ORDER BY s.createdAt DESC",
            SubscriptionEntity.class
        ).setParameter("status", status)
         .getResultList();

        return entities.stream()
            .map(SubscriptionEntity::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Subscription> findActiveByCustomerId(CustomerId customerId) {
        List<SubscriptionEntity> entities = entityManager.createQuery(
            "SELECT s FROM SubscriptionEntity s WHERE s.customer.id = :customerId AND s.status = 'ACTIVE' ORDER BY s.createdAt DESC",
            SubscriptionEntity.class
        ).setParameter("customerId", customerId.value())
         .getResultList();

        return entities.stream()
            .map(SubscriptionEntity::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Subscription> findSuspendedByCustomerId(CustomerId customerId) {
        List<SubscriptionEntity> entities = entityManager.createQuery(
            "SELECT s FROM SubscriptionEntity s WHERE s.customer.id = :customerId AND s.status = 'SUSPENDED' ORDER BY s.createdAt DESC",
            SubscriptionEntity.class
        ).setParameter("customerId", customerId.value())
         .getResultList();

        return entities.stream()
            .map(SubscriptionEntity::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Subscription> findDueForRenewal(LocalDate date) {
        List<SubscriptionEntity> entities = entityManager.createQuery(
            "SELECT s FROM SubscriptionEntity s WHERE s.endDate <= :date AND s.status = 'ACTIVE' ORDER BY s.endDate ASC",
            SubscriptionEntity.class
        ).setParameter("date", date)
         .getResultList();

        return entities.stream()
            .map(SubscriptionEntity::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Subscription> findExpired() {
        List<SubscriptionEntity> entities = entityManager.createQuery(
            "SELECT s FROM SubscriptionEntity s WHERE s.endDate < :now AND s.status = 'ACTIVE' ORDER BY s.endDate DESC",
            SubscriptionEntity.class
        ).setParameter("now", LocalDate.now())
         .getResultList();

        return entities.stream()
            .map(SubscriptionEntity::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Subscription> findByAutoRenewTrue() {
        List<SubscriptionEntity> entities = entityManager.createQuery(
            "SELECT s FROM SubscriptionEntity s WHERE s.autoRenew = true AND s.status = 'ACTIVE' ORDER BY s.nextBillingDate ASC",
            SubscriptionEntity.class
        ).getResultList();

        return entities.stream()
            .map(SubscriptionEntity::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Subscription> findByBillingPeriod(String billingPeriod) {
        List<SubscriptionEntity> entities = entityManager.createQuery(
            "SELECT s FROM SubscriptionEntity s WHERE s.billingPeriod = :billingPeriod ORDER BY s.createdAt DESC",
            SubscriptionEntity.class
        ).setParameter("billingPeriod", billingPeriod)
         .getResultList();

        return entities.stream()
            .map(SubscriptionEntity::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public void deleteById(SubscriptionId id) {
        SubscriptionEntity entity = entityManager.find(SubscriptionEntity.class, id.value());
        if (entity != null) {
            entityManager.remove(entity);
        }
    }

    @Override
    public boolean existsById(SubscriptionId id) {
        Long count = entityManager.createQuery(
            "SELECT COUNT(s) FROM SubscriptionEntity s WHERE s.id = :id",
            Long.class
        ).setParameter("id", id.value())
         .getSingleResult();

        return count != null && count > 0;
    }

    @Override
    public boolean existsBySubscriptionNumber(String subscriptionNumber) {
        Long count = entityManager.createQuery(
            "SELECT COUNT(s) FROM SubscriptionEntity s WHERE s.subscriptionNumber = :subscriptionNumber",
            Long.class
        ).setParameter("subscriptionNumber", subscriptionNumber)
         .getSingleResult();

        return count != null && count > 0;
    }

    @Override
    public long countByCustomerId(CustomerId customerId) {
        Long count = entityManager.createQuery(
            "SELECT COUNT(s) FROM SubscriptionEntity s WHERE s.customer.id = :customerId",
            Long.class
        ).setParameter("customerId", customerId.value())
         .getSingleResult();

        return count != null ? count : 0;
    }

    @Override
    public long countActiveByCustomerId(CustomerId customerId) {
        Long count = entityManager.createQuery(
            "SELECT COUNT(s) FROM SubscriptionEntity s WHERE s.customer.id = :customerId AND s.status = 'ACTIVE'",
            Long.class
        ).setParameter("customerId", customerId.value())
         .getSingleResult();

        return count != null ? count : 0;
    }
}
