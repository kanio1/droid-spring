package com.droid.bss.domain.invoice;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Read-only repository for Invoice entity queries
 */
public interface InvoiceReadRepository {

    /**
     * Find invoice by ID
     */
    Optional<InvoiceEntity> findById(UUID id);

    /**
     * Find invoice by invoice number
     */
    Optional<InvoiceEntity> findByInvoiceNumber(String invoiceNumber);

    /**
     * Find all invoices with pagination
     */
    Page<InvoiceEntity> findAll(Pageable pageable);

    /**
     * Find invoices by status
     */
    Page<InvoiceEntity> findByStatus(InvoiceStatus status, Pageable pageable);

    /**
     * Find invoices by type
     */
    Page<InvoiceEntity> findByInvoiceType(InvoiceType invoiceType, Pageable pageable);

    /**
     * Find invoices by customer ID
     */
    Page<InvoiceEntity> findByCustomerId(UUID customerId, Pageable pageable);

    /**
     * Find invoices issued between dates
     */
    Page<InvoiceEntity> findByIssueDateBetween(java.time.LocalDate startDate, java.time.LocalDate endDate, Pageable pageable);

    /**
     * Find invoices due between dates
     */
    Page<InvoiceEntity> findByDueDateBetween(java.time.LocalDate startDate, java.time.LocalDate endDate, Pageable pageable);

    /**
     * Find invoices by status and due date before
     */
    Page<InvoiceEntity> findByStatusAndDueDateBefore(InvoiceStatus status, java.time.LocalDate date, Pageable pageable);

    /**
     * Find invoices by status in list
     */
    Page<InvoiceEntity> findByStatusIn(List<InvoiceStatus> statuses, Pageable pageable);

    /**
     * Find invoices by total amount range
     */
    Page<InvoiceEntity> findByTotalAmountBetween(java.math.BigDecimal minAmount, java.math.BigDecimal maxAmount, Pageable pageable);

    /**
     * Find invoices by currency
     */
    Page<InvoiceEntity> findByCurrency(String currency, Pageable pageable);

    /**
     * Search invoices by text
     */
    Page<InvoiceEntity> search(String query, Pageable pageable);
}
