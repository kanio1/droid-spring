package com.droid.bss.infrastructure.write;

import com.droid.bss.domain.order.*;
import com.droid.bss.domain.order.event.OrderEventPublisher;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Repository
@Transactional
public class OrderRepositoryImpl implements OrderRepository {

    @PersistenceContext
    private EntityManager entityManager;

    private final OrderEventPublisher eventPublisher;

    public OrderRepositoryImpl(OrderEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Order save(Order order) {
        OrderEntity entity = OrderEntity.from(order);

        // Check if this is a new entity
        Long count = entityManager.createQuery(
            "SELECT COUNT(o) FROM OrderEntity o WHERE o.id = :id",
            Long.class
        ).setParameter("id", entity.getId())
         .getSingleResult();

        boolean exists = count != null && count > 0;

        // Save using merge
        OrderEntity merged = entityManager.merge(entity);
        entityManager.flush();

        // Publish event
        if (exists) {
            // Order was updated
            eventPublisher.publishOrderUpdated(merged);
        } else {
            // Order was created
            eventPublisher.publishOrderCreated(merged);
        }

        // Return as domain
        return merged.toDomain();
    }

    @Override
    public Optional<Order> findById(OrderId id) {
        OrderEntity entity = entityManager.find(OrderEntity.class, id.value());
        return entity != null ? Optional.of(entity.toDomain()) : Optional.empty();
    }

    @Override
    public Optional<Order> findByOrderNumber(String orderNumber) {
        try {
            OrderEntity entity = entityManager.createQuery(
                "SELECT o FROM OrderEntity o WHERE o.orderNumber = :orderNumber",
                OrderEntity.class
            ).setParameter("orderNumber", orderNumber)
             .getSingleResult();

            return Optional.of(entity.toDomain());
        } catch (jakarta.persistence.NoResultException e) {
            return Optional.empty();
        }
    }

    @Override
    public List<Order> findByCustomerId(com.droid.bss.domain.customer.CustomerId customerId) {
        List<OrderEntity> entities = entityManager.createQuery(
            "SELECT o FROM OrderEntity o WHERE o.customer.id = :customerId ORDER BY o.createdAt DESC",
            OrderEntity.class
        ).setParameter("customerId", customerId.value())
         .getResultList();

        return entities.stream()
            .map(OrderEntity::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public void deleteById(OrderId id) {
        OrderEntity entity = entityManager.find(OrderEntity.class, id.value());
        if (entity != null) {
            entityManager.remove(entity);
        }
    }

    @Override
    public boolean existsById(OrderId id) {
        Long count = entityManager.createQuery(
            "SELECT COUNT(o) FROM OrderEntity o WHERE o.id = :id",
            Long.class
        ).setParameter("id", id.value())
         .getSingleResult();

        return count != null && count > 0;
    }

    @Override
    public boolean existsByOrderNumber(String orderNumber) {
        Long count = entityManager.createQuery(
            "SELECT COUNT(o) FROM OrderEntity o WHERE o.orderNumber = :orderNumber",
            Long.class
        ).setParameter("orderNumber", orderNumber)
         .getSingleResult();

        return count != null && count > 0;
    }
}
