package com.droid.bss.domain.invoice.repository;

import com.droid.bss.domain.invoice.InvoiceEntity;
import com.droid.bss.domain.invoice.InvoiceStatus;
import com.droid.bss.domain.invoice.InvoiceType;
import com.droid.bss.domain.customer.CustomerEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for InvoiceEntity
 */
@Repository
public interface InvoiceEntityRepository extends JpaRepository<InvoiceEntity, UUID> {

    /**
     * Find invoice by invoice number
     */
    Optional<InvoiceEntity> findByInvoiceNumber(String invoiceNumber);

    /**
     * Find invoices by customer
     */
    Page<InvoiceEntity> findByCustomer(CustomerEntity customer, Pageable pageable);

    /**
     * Find invoices by customer ID
     */
    @Query("SELECT i FROM InvoiceEntity i WHERE i.customer.id = :customerId")
    Page<InvoiceEntity> findByCustomerId(@Param("customerId") UUID customerId, Pageable pageable);

    /**
     * Find invoices by status
     */
    Page<InvoiceEntity> findByStatus(InvoiceStatus status, Pageable pageable);

    /**
     * Find invoices by type
     */
    Page<InvoiceEntity> findByInvoiceType(InvoiceType invoiceType, Pageable pageable);

    /**
     * Find invoices by issue date range (alias for findByIssueDateBetween)
     */
    @Query("SELECT i FROM InvoiceEntity i WHERE i.issueDate BETWEEN :startDate AND :endDate")
    Page<InvoiceEntity> findByIssueDateBetween(@Param("startDate") LocalDate startDate,
                                              @Param("endDate") LocalDate endDate,
                                              Pageable pageable);

    /**
     * Find invoices by issue date range (alias)
     */
    @Query("SELECT i FROM InvoiceEntity i WHERE i.issueDate BETWEEN :startDate AND :endDate")
    Page<InvoiceEntity> findByIssueDateRange(@Param("startDate") LocalDate startDate,
                                             @Param("endDate") LocalDate endDate,
                                             Pageable pageable);

    /**
     * Find invoices by due date range
     */
    @Query("SELECT i FROM InvoiceEntity i WHERE i.dueDate BETWEEN :startDate AND :endDate")
    Page<InvoiceEntity> findByDueDateBetween(@Param("startDate") LocalDate startDate,
                                            @Param("endDate") LocalDate endDate,
                                            Pageable pageable);

    /**
     * Find invoices by due date range (alias)
     */
    @Query("SELECT i FROM InvoiceEntity i WHERE i.dueDate BETWEEN :startDate AND :endDate")
    Page<InvoiceEntity> findByDueDateRange(@Param("startDate") LocalDate startDate,
                                           @Param("endDate") LocalDate endDate,
                                           Pageable pageable);

    /**
     * Find invoices by status and due date before
     */
    @Query("SELECT i FROM InvoiceEntity i WHERE i.status = :status AND i.dueDate < :date")
    Page<InvoiceEntity> findByStatusAndDueDateBefore(@Param("status") InvoiceStatus status,
                                                    @Param("date") LocalDate date,
                                                    Pageable pageable);

    /**
     * Find invoices by status in list
     */
    @Query("SELECT i FROM InvoiceEntity i WHERE i.status IN :statuses")
    Page<InvoiceEntity> findByStatusIn(@Param("statuses") List<InvoiceStatus> statuses, Pageable pageable);

    /**
     * Find invoices by total amount range
     */
    @Query("SELECT i FROM InvoiceEntity i WHERE i.totalAmount BETWEEN :minAmount AND :maxAmount")
    Page<InvoiceEntity> findByTotalAmountBetween(@Param("minAmount") BigDecimal minAmount,
                                                @Param("maxAmount") BigDecimal maxAmount,
                                                Pageable pageable);

    /**
     * Find invoices by currency
     */
    Page<InvoiceEntity> findByCurrency(String currency, Pageable pageable);

    /**
     * Search invoices by invoice number, customer name, or notes
     */
    @Query("SELECT i FROM InvoiceEntity i WHERE " +
           "LOWER(i.invoiceNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(i.notes) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(i.customer.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(i.customer.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<InvoiceEntity> search(@Param("searchTerm") String searchTerm, Pageable pageable);

    /**
     * Count invoices by status
     */
    long countByStatus(InvoiceStatus status);

    /**
     * Check if invoice number exists
     */
    boolean existsByInvoiceNumber(String invoiceNumber);

    /**
     * Find recent invoices
     */
    @Query("SELECT i FROM InvoiceEntity i ORDER BY i.createdAt DESC")
    Page<InvoiceEntity> findRecent(Pageable pageable);

    /**
     * Find unpaid invoices (issued, sent, or overdue)
     */
    @Query("SELECT i FROM InvoiceEntity i WHERE i.status IN :statuses")
    List<InvoiceEntity> findUnpaid(@Param("statuses") List<InvoiceStatus> statuses);

    /**
     * Find unpaid invoices with pagination (for test compatibility)
     */
    @Query("SELECT i FROM InvoiceEntity i WHERE i.status IN :statuses")
    Page<InvoiceEntity> findUnpaidInvoices(@Param("statuses") List<InvoiceStatus> statuses, Pageable pageable);

    /**
     * Find overdue invoices
     */
    @Query("SELECT i FROM InvoiceEntity i WHERE i.status IN :statuses AND i.dueDate < :currentDate")
    Page<InvoiceEntity> findOverdueInvoices(@Param("statuses") List<InvoiceStatus> statuses,
                                           @Param("currentDate") LocalDate currentDate,
                                           Pageable pageable);

    /**
     * Find paid invoices
     */
    @Query("SELECT i FROM InvoiceEntity i WHERE i.status = :status")
    Page<InvoiceEntity> findPaidInvoices(@Param("status") InvoiceStatus status, Pageable pageable);

    /**
     * Find sent invoices
     */
    @Query("SELECT i FROM InvoiceEntity i WHERE i.status = :status")
    Page<InvoiceEntity> findSentInvoices(@Param("status") InvoiceStatus status, Pageable pageable);

    /**
     * Find sent invoices (without status parameter)
     */
    default Page<InvoiceEntity> findSentInvoices(Pageable pageable) {
        return findSentInvoices(InvoiceStatus.SENT, pageable);
    }

    /**
     * Find recent invoices (alias for findRecent)
     */
    @Query("SELECT i FROM InvoiceEntity i ORDER BY i.createdAt DESC")
    Page<InvoiceEntity> findRecentInvoices(Pageable pageable);

    /**
     * Find invoices with total amount greater than
     */
    @Query("SELECT i FROM InvoiceEntity i WHERE i.totalAmount > :amount")
    Page<InvoiceEntity> findByTotalAmountGreaterThan(@Param("amount") BigDecimal amount, Pageable pageable);

    /**
     * Find invoices by billing period
     */
    @Query("SELECT i FROM InvoiceEntity i WHERE i.billingPeriodStart BETWEEN :startDate AND :endDate OR i.billingPeriodEnd BETWEEN :startDate AND :endDate")
    Page<InvoiceEntity> findByBillingPeriod(@Param("startDate") LocalDate startDate,
                                           @Param("endDate") LocalDate endDate,
                                           Pageable pageable);

    /**
     * Find invoices to send (with specific status)
     */
    @Query("SELECT i FROM InvoiceEntity i WHERE i.status = :status")
    List<InvoiceEntity> findInvoicesToSend(@Param("status") InvoiceStatus status);

    /**
     * Search invoices (alias for search)
     */
    @Query("SELECT i FROM InvoiceEntity i WHERE " +
           "LOWER(i.invoiceNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(i.notes) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(i.customer.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(i.customer.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<InvoiceEntity> searchInvoices(@Param("searchTerm") String searchTerm, Pageable pageable);
}
