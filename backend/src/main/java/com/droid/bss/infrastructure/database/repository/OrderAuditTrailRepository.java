package com.droid.bss.infrastructure.database.repository;

import com.droid.bss.domain.order.OrderAuditTrail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for order audit trail
 */
@Repository
public interface OrderAuditTrailRepository extends JpaRepository<OrderAuditTrail, Long> {

    /**
     * Find all audit entries for an order, ordered by timestamp descending
     */
    List<OrderAuditTrail> findByOrderIdOrderByTimestampDesc(String orderId);

    /**
     * Find the latest audit entry for an order
     */
    Optional<OrderAuditTrail> findTopByOrderIdOrderByTimestampDesc(String orderId);

    /**
     * Find all audit entries for an order by action
     */
    List<OrderAuditTrail> findByOrderIdAndActionOrderByTimestampDesc(String orderId, String action);

    /**
     * Count audit entries for an order
     */
    long countByOrderId(String orderId);

    /**
     * Find all audit entries for orders by customer
     */
    List<OrderAuditTrail> findByOrder_Customer_IdOrderByTimestampDesc(String customerId);
}
