package com.droid.bss.domain.invoice.repository;

import com.droid.bss.domain.invoice.InvoiceEntity;
import com.droid.bss.domain.invoice.InvoiceStatus;
import com.droid.bss.domain.invoice.InvoiceType;
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
 * Repository for InvoiceEntity
 */
@Repository
public interface InvoiceRepository extends JpaRepository<InvoiceEntity, UUID> {

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
     * Find unpaid invoices
     */
    @Query("SELECT i FROM InvoiceEntity i WHERE i.status IN (:status1, :status2, :status3)")
    Page<InvoiceEntity> findUnpaidInvoices(@Param("status1") InvoiceStatus status1,
                                           @Param("status2") InvoiceStatus status2,
                                           @Param("status3") InvoiceStatus status3,
                                           Pageable pageable);

    /**
     * Find overdue invoices
     */
    @Query("SELECT i FROM InvoiceEntity i WHERE i.status = :overdueStatus OR " +
           "(i.status = :sentStatus AND i.dueDate < CURRENT_DATE)")
    Page<InvoiceEntity> findOverdueInvoices(@Param("overdueStatus") InvoiceStatus overdueStatus,
                                           @Param("sentStatus") InvoiceStatus sentStatus,
                                           Pageable pageable);

    /**
     * Find invoices by date range
     */
    @Query("SELECT i FROM InvoiceEntity i WHERE i.issueDate BETWEEN :startDate AND :endDate")
    Page<InvoiceEntity> findByIssueDateRange(@Param("startDate") LocalDate startDate,
                                             @Param("endDate") LocalDate endDate,
                                             Pageable pageable);

    /**
     * Find invoices by due date range
     */
    @Query("SELECT i FROM InvoiceEntity i WHERE i.dueDate BETWEEN :startDate AND :endDate")
    Page<InvoiceEntity> findByDueDateRange(@Param("startDate") LocalDate startDate,
                                          @Param("endDate") LocalDate endDate,
                                          Pageable pageable);

    /**
     * Search invoices by invoice number or notes
     */
    @Query("SELECT i FROM InvoiceEntity i WHERE " +
           "LOWER(i.invoiceNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(i.notes) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<InvoiceEntity> searchInvoices(@Param("searchTerm") String searchTerm, Pageable pageable);

    /**
     * Find paid invoices
     */
    @Query("SELECT i FROM InvoiceEntity i WHERE i.status = :status")
    Page<InvoiceEntity> findPaidInvoices(@Param("status") InvoiceStatus status, Pageable pageable);

    /**
     * Count invoices by status
     */
    long countByStatus(InvoiceStatus status);

    /**
     * Count invoices by customer
     */
    long countByCustomer(CustomerEntity customer);

    /**
     * Check if invoice number exists
     */
    boolean existsByInvoiceNumber(String invoiceNumber);

    /**
     * Find invoices sent via email
     */
    @Query("SELECT i FROM InvoiceEntity i WHERE i.sentToEmail IS NOT NULL")
    Page<InvoiceEntity> findSentInvoices(Pageable pageable);

    /**
     * Find recent invoices
     */
    @Query("SELECT i FROM InvoiceEntity i ORDER BY i.createdAt DESC")
    Page<InvoiceEntity> findRecentInvoices(Pageable pageable);

    /**
     * Find invoices with total amount greater than
     */
    @Query("SELECT i FROM InvoiceEntity i WHERE i.totalAmount > :amount")
    Page<InvoiceEntity> findByTotalAmountGreaterThan(@Param("amount") Double amount, Pageable pageable);

    /**
     * Find invoices by billing period
     */
    @Query("SELECT i FROM InvoiceEntity i WHERE i.billingPeriodStart = :periodStart " +
           "AND i.billingPeriodEnd = :periodEnd")
    Page<InvoiceEntity> findByBillingPeriod(@Param("periodStart") LocalDate periodStart,
                                           @Param("periodEnd") LocalDate periodEnd,
                                           Pageable pageable);

    /**
     * Find invoices needing to be sent
     */
    @Query("SELECT i FROM InvoiceEntity i WHERE i.status = :status")
    List<InvoiceEntity> findInvoicesToSend(@Param("status") InvoiceStatus status);

    /**
     * Find invoices by status with items (optimized to avoid N+1 queries)
     */
    @EntityGraph(attributePaths = {"items"})
    @Query("SELECT i FROM InvoiceEntity i WHERE i.status = :status")
    Page<InvoiceEntity> findByStatusWithItems(@Param("status") InvoiceStatus status, Pageable pageable);

    /**
     * Find all invoices with items (optimized to avoid N+1 queries)
     */
    @EntityGraph(attributePaths = {"items"})
    @Query("SELECT i FROM InvoiceEntity i")
    Page<InvoiceEntity> findAllWithItems(Pageable pageable);

    /**
     * Find recent invoices with items (optimized to avoid N+1 queries)
     */
    @EntityGraph(attributePaths = {"items"})
    @Query("SELECT i FROM InvoiceEntity i ORDER BY i.createdAt DESC")
    Page<InvoiceEntity> findRecentInvoicesWithItems(Pageable pageable);
}
