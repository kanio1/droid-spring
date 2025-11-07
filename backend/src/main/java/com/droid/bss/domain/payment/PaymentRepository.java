package com.droid.bss.domain.payment;

import com.droid.bss.domain.customer.CustomerId;
import com.droid.bss.domain.invoice.InvoiceId;

import java.util.List;
import java.util.Optional;

/**
 * PaymentRepository - DDD Port for Payment Aggregate
 * This is the interface (port) that the domain depends on.
 * The implementation will be in the infrastructure layer.
 */
public interface PaymentRepository {

    /**
     * Find payment by ID
     */
    Optional<Payment> findById(PaymentId id);

    /**
     * Find payment by payment number
     */
    Optional<Payment> findByPaymentNumber(String paymentNumber);

    /**
     * Find payment by transaction ID
     */
    Optional<Payment> findByTransactionId(String transactionId);

    /**
     * Find payments by customer
     */
    List<Payment> findByCustomerId(CustomerId customerId);

    /**
     * Find payments by invoice
     */
    List<Payment> findByInvoiceId(InvoiceId invoiceId);

    /**
     * Find payments by status
     */
    List<Payment> findByStatus(PaymentStatus status);

    /**
     * Find payments by payment method
     */
    List<Payment> findByPaymentMethod(PaymentMethod paymentMethod);

    /**
     * Save payment (create or update)
     */
    Payment save(Payment payment);

    /**
     * Delete payment by ID
     */
    void deleteById(PaymentId id);

    /**
     * Check if payment exists by ID
     */
    boolean existsById(PaymentId id);

    /**
     * Check if payment number exists
     */
    boolean existsByPaymentNumber(String paymentNumber);

    /**
     * Check if transaction ID exists
     */
    boolean existsByTransactionId(String transactionId);
}
