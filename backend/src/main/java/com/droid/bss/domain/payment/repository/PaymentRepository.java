package com.droid.bss.domain.payment.repository;

import com.droid.bss.domain.payment.PaymentEntity;
import com.droid.bss.domain.payment.PaymentStatus;
import com.droid.bss.domain.payment.PaymentMethod;
import com.droid.bss.domain.customer.CustomerEntity;
import com.droid.bss.domain.invoice.InvoiceEntity;
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
 * Repository for PaymentEntity
 */
@Repository
public interface PaymentRepository extends JpaRepository<PaymentEntity, UUID> {

    /**
     * Find payment by payment number
     */
    Optional<PaymentEntity> findByPaymentNumber(String paymentNumber);

    /**
     * Find payments by customer
     */
    Page<PaymentEntity> findByCustomer(CustomerEntity customer, Pageable pageable);

    /**
     * Find payments by customer ID
     */
    @Query("SELECT p FROM PaymentEntity p WHERE p.customer.id = :customerId")
    Page<PaymentEntity> findByCustomerId(@Param("customerId") UUID customerId, Pageable pageable);

    /**
     * Find payments by invoice
     */
    Page<PaymentEntity> findByInvoice(InvoiceEntity invoice, Pageable pageable);

    /**
     * Find payments by invoice ID
     */
    @Query("SELECT p FROM PaymentEntity p WHERE p.invoice.id = :invoiceId")
    Page<PaymentEntity> findByInvoiceId(@Param("invoiceId") UUID invoiceId, Pageable pageable);

    /**
     * Find payments by status
     */
    Page<PaymentEntity> findByPaymentStatus(PaymentStatus paymentStatus, Pageable pageable);

    /**
     * Find payments by method
     */
    Page<PaymentEntity> findByPaymentMethod(PaymentMethod paymentMethod, Pageable pageable);

    /**
     * Find completed payments
     */
    @Query("SELECT p FROM PaymentEntity p WHERE p.paymentStatus = :status")
    Page<PaymentEntity> findCompletedPayments(@Param("status") PaymentStatus status, Pageable pageable);

    /**
     * Find pending payments
     */
    @Query("SELECT p FROM PaymentEntity p WHERE p.paymentStatus = :status")
    Page<PaymentEntity> findPendingPayments(@Param("status") PaymentStatus status, Pageable pageable);

    /**
     * Find payments by date range
     */
    @Query("SELECT p FROM PaymentEntity p WHERE p.paymentDate BETWEEN :startDate AND :endDate")
    Page<PaymentEntity> findByPaymentDateRange(@Param("startDate") LocalDate startDate,
                                               @Param("endDate") LocalDate endDate,
                                               Pageable pageable);

    /**
     * Search payments by payment number or reference number
     */
    @Query("SELECT p FROM PaymentEntity p WHERE " +
           "LOWER(p.paymentNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(p.referenceNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<PaymentEntity> searchPayments(@Param("searchTerm") String searchTerm, Pageable pageable);

    /**
     * Find payments by gateway
     */
    @Query("SELECT p FROM PaymentEntity p WHERE p.gateway = :gateway")
    Page<PaymentEntity> findByGateway(@Param("gateway") String gateway, Pageable pageable);

    /**
     * Find payments by transaction ID
     */
    Optional<PaymentEntity> findByTransactionId(String transactionId);

    /**
     * Count payments by status
     */
    long countByPaymentStatus(PaymentStatus paymentStatus);

    /**
     * Count payments by customer
     */
    long countByCustomer(CustomerEntity customer);

    /**
     * Check if payment number exists
     */
    boolean existsByPaymentNumber(String paymentNumber);

    /**
     * Find failed payments that can be retried
     */
    @Query("SELECT p FROM PaymentEntity p WHERE p.paymentStatus = :status")
    Page<PaymentEntity> findFailedPayments(@Param("status") PaymentStatus status, Pageable pageable);

    /**
     * Find refunded payments
     */
    @Query("SELECT p FROM PaymentEntity p WHERE p.paymentStatus = :status")
    Page<PaymentEntity> findRefundedPayments(@Param("status") PaymentStatus status, Pageable pageable);

    /**
     * Find payments by amount range
     */
    @Query("SELECT p FROM PaymentEntity p WHERE p.amount BETWEEN :minAmount AND :maxAmount")
    Page<PaymentEntity> findByAmountRange(@Param("minAmount") Double minAmount,
                                          @Param("maxAmount") Double maxAmount,
                                          Pageable pageable);

    /**
     * Calculate total payments by customer and date range
     */
    @Query("SELECT SUM(p.amount) FROM PaymentEntity p WHERE p.customer.id = :customerId " +
           "AND p.paymentDate BETWEEN :startDate AND :endDate " +
           "AND p.paymentStatus = :status")
    Double sumPaymentsByCustomerAndDateRange(@Param("customerId") UUID customerId,
                                             @Param("startDate") LocalDate startDate,
                                             @Param("endDate") LocalDate endDate,
                                             @Param("status") PaymentStatus status);

    /**
     * Find payments by method and status
     */
    @Query("SELECT p FROM PaymentEntity p WHERE p.paymentMethod = :method " +
           "AND p.paymentStatus = :status")
    Page<PaymentEntity> findByPaymentMethodAndStatus(@Param("method") PaymentMethod method,
                                                      @Param("status") PaymentStatus status,
                                                      Pageable pageable);
}
