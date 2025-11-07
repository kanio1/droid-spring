package com.droid.bss.infrastructure.write;

import com.droid.bss.domain.payment.*;
import com.droid.bss.domain.payment.event.PaymentEventPublisher;
import com.droid.bss.domain.invoice.InvoiceId;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Repository
@Transactional
public class PaymentRepositoryImpl implements PaymentRepository {

    @PersistenceContext
    private EntityManager entityManager;

    private final PaymentEventPublisher eventPublisher;

    public PaymentRepositoryImpl(PaymentEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Payment save(Payment payment) {
        PaymentEntity entity = PaymentEntity.from(payment);
        PaymentStatus previousStatus = entity.getPaymentStatus();

        // Check if this is a new entity
        Long count = entityManager.createQuery(
            "SELECT COUNT(p) FROM PaymentEntity p WHERE p.id = :id",
            Long.class
        ).setParameter("id", entity.getId())
         .getSingleResult();

        boolean exists = count != null && count > 0;

        // Save using merge
        PaymentEntity merged = entityManager.merge(entity);
        entityManager.flush();

        // Publish event (only for new payments for now)
        if (!exists) {
            // Payment was created
            try {
                eventPublisher.publishPaymentCreated(merged);
            } catch (Exception e) {
                // Log but don't fail
                System.err.println("Failed to publish event: " + e.getMessage());
            }
        }

        // Return as domain
        return merged.toDomain();
    }

    @Override
    public Optional<Payment> findById(PaymentId id) {
        PaymentEntity entity = entityManager.find(PaymentEntity.class, id.value());
        return entity != null ? Optional.of(entity.toDomain()) : Optional.empty();
    }

    @Override
    public Optional<Payment> findByPaymentNumber(String paymentNumber) {
        try {
            PaymentEntity entity = entityManager.createQuery(
                "SELECT p FROM PaymentEntity p WHERE p.paymentNumber = :paymentNumber",
                PaymentEntity.class
            ).setParameter("paymentNumber", paymentNumber)
             .getSingleResult();

            return Optional.of(entity.toDomain());
        } catch ( jakarta.persistence.NoResultException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<Payment> findByTransactionId(String transactionId) {
        try {
            PaymentEntity entity = entityManager.createQuery(
                "SELECT p FROM PaymentEntity p WHERE p.transactionId = :transactionId",
                PaymentEntity.class
            ).setParameter("transactionId", transactionId)
             .getSingleResult();

            return Optional.of(entity.toDomain());
        } catch ( jakarta.persistence.NoResultException e) {
            return Optional.empty();
        }
    }

    @Override
    public List<Payment> findByCustomerId(com.droid.bss.domain.customer.CustomerId customerId) {
        List<PaymentEntity> entities = entityManager.createQuery(
            "SELECT p FROM PaymentEntity p WHERE p.customer.id = :customerId ORDER BY p.createdAt DESC",
            PaymentEntity.class
        ).setParameter("customerId", customerId.value())
         .getResultList();

        return entities.stream()
            .map(PaymentEntity::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Payment> findByInvoiceId(InvoiceId invoiceId) {
        List<PaymentEntity> entities = entityManager.createQuery(
            "SELECT p FROM PaymentEntity p WHERE p.invoice.id = :invoiceId ORDER BY p.createdAt DESC",
            PaymentEntity.class
        ).setParameter("invoiceId", invoiceId.value())
         .getResultList();

        return entities.stream()
            .map(PaymentEntity::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Payment> findByStatus(PaymentStatus status) {
        List<PaymentEntity> entities = entityManager.createQuery(
            "SELECT p FROM PaymentEntity p WHERE p.paymentStatus = :status ORDER BY p.createdAt DESC",
            PaymentEntity.class
        ).setParameter("status", status)
         .getResultList();

        return entities.stream()
            .map(PaymentEntity::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Payment> findByPaymentMethod(PaymentMethod paymentMethod) {
        List<PaymentEntity> entities = entityManager.createQuery(
            "SELECT p FROM PaymentEntity p WHERE p.paymentMethod = :paymentMethod ORDER BY p.createdAt DESC",
            PaymentEntity.class
        ).setParameter("paymentMethod", paymentMethod)
         .getResultList();

        return entities.stream()
            .map(PaymentEntity::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public void deleteById(PaymentId id) {
        PaymentEntity entity = entityManager.find(PaymentEntity.class, id.value());
        if (entity != null) {
            entityManager.remove(entity);
        }
    }

    @Override
    public boolean existsById(PaymentId id) {
        Long count = entityManager.createQuery(
            "SELECT COUNT(p) FROM PaymentEntity p WHERE p.id = :id",
            Long.class
        ).setParameter("id", id.value())
         .getSingleResult();

        return count != null && count > 0;
    }

    @Override
    public boolean existsByPaymentNumber(String paymentNumber) {
        Long count = entityManager.createQuery(
            "SELECT COUNT(p) FROM PaymentEntity p WHERE p.paymentNumber = :paymentNumber",
            Long.class
        ).setParameter("paymentNumber", paymentNumber)
         .getSingleResult();

        return count != null && count > 0;
    }

    @Override
    public boolean existsByTransactionId(String transactionId) {
        Long count = entityManager.createQuery(
            "SELECT COUNT(p) FROM PaymentEntity p WHERE p.transactionId = :transactionId",
            Long.class
        ).setParameter("transactionId", transactionId)
         .getSingleResult();

        return count != null && count > 0;
    }
}
