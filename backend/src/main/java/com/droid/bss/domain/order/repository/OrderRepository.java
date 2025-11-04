package com.droid.bss.domain.order.repository;

import com.droid.bss.domain.order.OrderEntity;
import com.droid.bss.domain.order.OrderStatus;
import com.droid.bss.domain.order.OrderType;
import com.droid.bss.domain.order.OrderPriority;
import com.droid.bss.domain.customer.CustomerEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for OrderEntity
 */
@Repository
public interface OrderRepository extends JpaRepository<OrderEntity, UUID> {

    /**
     * Find order by order number
     */
    Optional<OrderEntity> findByOrderNumber(String orderNumber);

    /**
     * Find orders by customer
     */
    Page<OrderEntity> findByCustomer(CustomerEntity customer, Pageable pageable);

    /**
     * Find orders by customer ID
     */
    @Query("SELECT o FROM OrderEntity o WHERE o.customer.id = :customerId")
    Page<OrderEntity> findByCustomerId(@Param("customerId") UUID customerId, Pageable pageable);

    /**
     * Find orders by status
     */
    Page<OrderEntity> findByStatus(OrderStatus status, Pageable pageable);

    /**
     * Find orders by type
     */
    Page<OrderEntity> findByOrderType(OrderType orderType, Pageable pageable);

    /**
     * Find orders by priority
     */
    Page<OrderEntity> findByPriority(OrderPriority priority, Pageable pageable);

    /**
     * Find pending orders
     */
    @Query("SELECT o FROM OrderEntity o WHERE o.status = :status")
    Page<OrderEntity> findPendingOrders(@Param("status") OrderStatus status, Pageable pageable);

    /**
     * Find orders by date range
     */
    @Query("SELECT o FROM OrderEntity o WHERE o.requestedDate BETWEEN :startDate AND :endDate")
    Page<OrderEntity> findByDateRange(@Param("startDate") LocalDate startDate,
                                      @Param("endDate") LocalDate endDate,
                                      Pageable pageable);

    /**
     * Find overdue orders (promised date passed but not completed)
     */
    @Query("SELECT o FROM OrderEntity o WHERE o.promisedDate < CURRENT_DATE " +
           "AND o.status NOT IN (:completed, :cancelled)")
    List<OrderEntity> findOverdueOrders(@Param("completed") OrderStatus completed,
                                        @Param("cancelled") OrderStatus cancelled);

    /**
     * Search orders by order number or notes
     */
    @Query("SELECT o FROM OrderEntity o WHERE " +
           "LOWER(o.orderNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(o.notes) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<OrderEntity> searchOrders(@Param("searchTerm") String searchTerm, Pageable pageable);

    /**
     * Find orders by sales channel
     */
    @Query("SELECT o FROM OrderEntity o WHERE o.orderChannel = :channel")
    Page<OrderEntity> findByOrderChannel(@Param("channel") String channel, Pageable pageable);

    /**
     * Find orders by sales rep
     */
    @Query("SELECT o FROM OrderEntity o WHERE o.salesRepId = :salesRepId")
    Page<OrderEntity> findBySalesRepId(@Param("salesRepId") String salesRepId, Pageable pageable);

    /**
     * Count orders by status
     */
    long countByStatus(OrderStatus status);

    /**
     * Count orders by customer
     */
    long countByCustomer(CustomerEntity customer);

    /**
     * Check if order number exists
     */
    boolean existsByOrderNumber(String orderNumber);

    /**
     * Find recent orders
     */
    @Query("SELECT o FROM OrderEntity o ORDER BY o.createdAt DESC")
    Page<OrderEntity> findRecentOrders(Pageable pageable);

    /**
     * Find orders with total amount greater than
     */
    @Query("SELECT o FROM OrderEntity o WHERE o.totalAmount > :amount")
    Page<OrderEntity> findByTotalAmountGreaterThan(@Param("amount") Double amount, Pageable pageable);

    /**
     * Find orders by status with items (optimized to avoid N+1 queries)
     */
    @EntityGraph(attributePaths = {"items"})
    @Query("SELECT o FROM OrderEntity o WHERE o.status = :status")
    Page<OrderEntity> findByStatusWithItems(@Param("status") OrderStatus status, Pageable pageable);

    /**
     * Find all orders with items (optimized to avoid N+1 queries)
     */
    @EntityGraph(attributePaths = {"items"})
    @Query("SELECT o FROM OrderEntity o")
    Page<OrderEntity> findAllWithItems(Pageable pageable);

    /**
     * Find recent orders with items (optimized to avoid N+1 queries)
     */
    @EntityGraph(attributePaths = {"items"})
    @Query("SELECT o FROM OrderEntity o ORDER BY o.createdAt DESC")
    Page<OrderEntity> findRecentOrdersWithItems(Pageable pageable);
}
