package com.droid.bss.domain.invoice;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository port for Invoice entity
 */
public interface InvoiceRepository {

    /**
     * Save invoice (create or update)
     */
    InvoiceEntity save(InvoiceEntity invoice);

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
    List<InvoiceEntity> findAll(int page, int size);

    /**
     * Find invoices by status
     */
    List<InvoiceEntity> findByStatus(InvoiceStatus status, int page, int size);

    /**
     * Find invoices by type
     */
    List<InvoiceEntity> findByInvoiceType(InvoiceType invoiceType, int page, int size);

    /**
     * Find invoices by customer ID
     */
    List<InvoiceEntity> findByCustomerId(UUID customerId, int page, int size);

    /**
     * Find invoices issued between dates
     */
    List<InvoiceEntity> findByIssueDateBetween(java.time.LocalDate startDate, java.time.LocalDate endDate, int page, int size);

    /**
     * Find invoices due between dates
     */
    List<InvoiceEntity> findByDueDateBetween(java.time.LocalDate startDate, java.time.LocalDate endDate, int page, int size);

    /**
     * Find invoices by total amount range
     */
    List<InvoiceEntity> findByTotalAmountBetween(java.math.BigDecimal minAmount, java.math.BigDecimal maxAmount, int page, int size);

    /**
     * Find invoices by currency
     */
    List<InvoiceEntity> findByCurrency(String currency, int page, int size);

    /**
     * Search invoices by text
     */
    List<InvoiceEntity> search(String query, int page, int size);

    /**
     * Count all invoices
     */
    long count();

    /**
     * Count invoices by status
     */
    long countByStatus(InvoiceStatus status);

    /**
     * Delete invoice by ID
     */
    boolean deleteById(UUID id);
}
