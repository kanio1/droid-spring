package com.droid.bss.domain.order;

import com.droid.bss.domain.customer.CustomerId;

import java.util.List;
import java.util.Optional;

/**
 * OrderRepository - DDD Port for Order Aggregate
 * This is the interface (port) that the domain depends on.
 * The implementation will be in the infrastructure layer.
 */
public interface OrderRepository {

    /**
     * Find order by ID
     */
    Optional<Order> findById(OrderId id);

    /**
     * Find order by order number
     */
    Optional<Order> findByOrderNumber(String orderNumber);

    /**
     * Find all orders for a customer
     */
    List<Order> findByCustomerId(CustomerId customerId);

    /**
     * Save order (create or update)
     */
    Order save(Order order);

    /**
     * Delete order by ID
     */
    void deleteById(OrderId id);

    /**
     * Check if order exists by ID
     */
    boolean existsById(OrderId id);

    /**
     * Check if order number exists
     */
    boolean existsByOrderNumber(String orderNumber);
}
