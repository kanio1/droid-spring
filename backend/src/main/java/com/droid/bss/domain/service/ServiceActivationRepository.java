package com.droid.bss.domain.service;

import com.droid.bss.domain.customer.Customer;
import com.droid.bss.domain.customer.CustomerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for ServiceActivationEntity
 */
@Repository
public interface ServiceActivationRepository extends JpaRepository<ServiceActivationEntity, UUID> {

    /**
     * Find active activations for a customer
     */
    @Query("SELECT sa FROM ServiceActivationEntity sa WHERE sa.customer = :customer AND sa.status = 'ACTIVE'")
    List<ServiceActivationEntity> findActiveByCustomer(@Param("customer") Customer customer);

    /**
     * Find pending activations for a customer
     */
    @Query("SELECT sa FROM ServiceActivationEntity sa WHERE sa.customer = :customer AND sa.status IN ('PENDING', 'SCHEDULED')")
    List<ServiceActivationEntity> findPendingByCustomer(@Param("customer") Customer customer);

    /**
     * Find activation by customer and service
     */
    @Query("SELECT sa FROM ServiceActivationEntity sa WHERE sa.customer = :customer AND sa.service = :service ORDER BY sa.createdAt DESC")
    Optional<ServiceActivationEntity> findByCustomerAndService(
            @Param("customer") Customer customer,
            @Param("service") ServiceEntity service);

    /**
     * Find all activations by status
     */
    @Query("SELECT sa FROM ServiceActivationEntity sa WHERE sa.status = :status ORDER BY sa.createdAt ASC")
    List<ServiceActivationEntity> findByStatus(@Param("status") ActivationStatus status);

    /**
     * Find scheduled activations that should be processed
     */
    @Query("SELECT sa FROM ServiceActivationEntity sa WHERE sa.status = 'SCHEDULED' AND sa.scheduledDate <= :now")
    List<ServiceActivationEntity> findScheduledForProcessing(@Param("now") LocalDateTime now);

    /**
     * Find activations by correlation ID
     */
    @Query("SELECT sa FROM ServiceActivationEntity sa WHERE sa.correlationId = :correlationId")
    Optional<ServiceActivationEntity> findByCorrelationId(@Param("correlationId") String correlationId);

    /**
     * Count active services per customer
     */
    @Query("SELECT COUNT(sa) FROM ServiceActivationEntity sa WHERE sa.customer = :customer AND sa.status = 'ACTIVE'")
    Long countActiveByCustomer(@Param("customer") CustomerEntity customer);
}
