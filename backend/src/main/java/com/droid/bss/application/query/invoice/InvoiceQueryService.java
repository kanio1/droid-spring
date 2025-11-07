package com.droid.bss.application.query.invoice;

import com.droid.bss.application.dto.common.PageResponse;
import com.droid.bss.application.dto.invoice.InvoiceResponse;
import com.droid.bss.domain.invoice.InvoiceEntity;
import com.droid.bss.domain.invoice.InvoiceReadRepository;
import com.droid.bss.domain.invoice.InvoiceStatus;
import com.droid.bss.domain.invoice.InvoiceType;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Query service for invoice-related queries
 */
@Service
@CacheConfig(cacheNames = "invoices")
public class InvoiceQueryService {

    private final InvoiceReadRepository invoiceReadRepository;

    public InvoiceQueryService(InvoiceReadRepository invoiceReadRepository) {
        this.invoiceReadRepository = invoiceReadRepository;
    }

    /**
     * Get invoice by ID
     */
    @Cacheable(key = "#id", unless = "#result == null")
    public Optional<InvoiceResponse> findById(UUID id) {
        return invoiceReadRepository.findById(id)
                .map(InvoiceResponse::from);
    }

    /**
     * Get all invoices with pagination
     */
    public PageResponse<InvoiceResponse> findAll(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<InvoiceEntity> invoicePage = invoiceReadRepository.findAll(pageable);

        List<InvoiceResponse> invoices = invoicePage.getContent().stream()
                .map(InvoiceResponse::from)
                .collect(Collectors.toList());

        return PageResponse.of(
                invoices,
                page,
                size,
                invoicePage.getTotalElements()
        );
    }

    /**
     * Find invoices by status
     */
    @Cacheable(key = "{#status, #page, #size}")
    public PageResponse<InvoiceResponse> findByStatus(InvoiceStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<InvoiceEntity> invoicePage = invoiceReadRepository.findByStatus(status, pageable);

        List<InvoiceResponse> invoices = invoicePage.getContent().stream()
                .map(InvoiceResponse::from)
                .collect(Collectors.toList());

        return PageResponse.of(
                invoices,
                page,
                size,
                invoicePage.getTotalElements()
        );
    }

    /**
     * Find invoices by type
     */
    public PageResponse<InvoiceResponse> findByInvoiceType(InvoiceType invoiceType, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<InvoiceEntity> invoicePage = invoiceReadRepository.findByInvoiceType(invoiceType, pageable);

        List<InvoiceResponse> invoices = invoicePage.getContent().stream()
                .map(InvoiceResponse::from)
                .collect(Collectors.toList());

        return PageResponse.of(
                invoices,
                page,
                size,
                invoicePage.getTotalElements()
        );
    }

    /**
     * Find invoices by customer ID
     */
    @Cacheable(key = "{#customerId, #page, #size}")
    public PageResponse<InvoiceResponse> findByCustomerId(String customerId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        UUID customerUuid = UUID.fromString(customerId);
        Page<InvoiceEntity> invoicePage = invoiceReadRepository.findByCustomerId(customerUuid, pageable);

        List<InvoiceResponse> invoices = invoicePage.getContent().stream()
                .map(InvoiceResponse::from)
                .collect(Collectors.toList());

        return PageResponse.of(
                invoices,
                page,
                size,
                invoicePage.getTotalElements()
        );
    }

    /**
     * Find invoices by invoice number
     */
    @Cacheable(key = "#invoiceNumber", unless = "#result == null")
    public Optional<InvoiceResponse> findByInvoiceNumber(String invoiceNumber) {
        return invoiceReadRepository.findByInvoiceNumber(invoiceNumber)
                .map(InvoiceResponse::from);
    }

    /**
     * Find invoices issued between dates
     */
    public PageResponse<InvoiceResponse> findByIssueDateBetween(LocalDate startDate, LocalDate endDate, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<InvoiceEntity> invoicePage = invoiceReadRepository.findByIssueDateBetween(startDate, endDate, pageable);

        List<InvoiceResponse> invoices = invoicePage.getContent().stream()
                .map(InvoiceResponse::from)
                .collect(Collectors.toList());

        return PageResponse.of(
                invoices,
                page,
                size,
                invoicePage.getTotalElements()
        );
    }

    /**
     * Find invoices due between dates
     */
    public PageResponse<InvoiceResponse> findByDueDateBetween(LocalDate startDate, LocalDate endDate, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<InvoiceEntity> invoicePage = invoiceReadRepository.findByDueDateBetween(startDate, endDate, pageable);

        List<InvoiceResponse> invoices = invoicePage.getContent().stream()
                .map(InvoiceResponse::from)
                .collect(Collectors.toList());

        return PageResponse.of(
                invoices,
                page,
                size,
                invoicePage.getTotalElements()
        );
    }

    /**
     * Find unpaid invoices
     */
    public PageResponse<InvoiceResponse> findUnpaid(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<InvoiceEntity> invoicePage = invoiceReadRepository.findByStatusIn(
                List.of(InvoiceStatus.ISSUED, InvoiceStatus.SENT, InvoiceStatus.OVERDUE),
                pageable
        );

        List<InvoiceResponse> invoices = invoicePage.getContent().stream()
                .map(InvoiceResponse::from)
                .collect(Collectors.toList());

        return PageResponse.of(
                invoices,
                page,
                size,
                invoicePage.getTotalElements()
        );
    }

    /**
     * Find overdue invoices
     */
    public PageResponse<InvoiceResponse> findOverdue(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<InvoiceEntity> invoicePage = invoiceReadRepository.findByStatusAndDueDateBefore(
                InvoiceStatus.OVERDUE,
                LocalDate.now(),
                pageable
        );

        List<InvoiceResponse> invoices = invoicePage.getContent().stream()
                .map(InvoiceResponse::from)
                .collect(Collectors.toList());

        return PageResponse.of(
                invoices,
                page,
                size,
                invoicePage.getTotalElements()
        );
    }

    /**
     * Search invoices by text (invoice number, customer name, notes)
     */
    public PageResponse<InvoiceResponse> search(String query, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<InvoiceEntity> invoicePage = invoiceReadRepository.search(query, pageable);

        List<InvoiceResponse> invoices = invoicePage.getContent().stream()
                .map(InvoiceResponse::from)
                .collect(Collectors.toList());

        return PageResponse.of(
                invoices,
                page,
                size,
                invoicePage.getTotalElements()
        );
    }

    /**
     * Find invoices by total amount range
     */
    public PageResponse<InvoiceResponse> findByTotalAmountBetween(BigDecimal minAmount, BigDecimal maxAmount, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<InvoiceEntity> invoicePage = invoiceReadRepository.findByTotalAmountBetween(minAmount, maxAmount, pageable);

        List<InvoiceResponse> invoices = invoicePage.getContent().stream()
                .map(InvoiceResponse::from)
                .collect(Collectors.toList());

        return PageResponse.of(
                invoices,
                page,
                size,
                invoicePage.getTotalElements()
        );
    }

    /**
     * Find invoices by currency
     */
    public PageResponse<InvoiceResponse> findByCurrency(String currency, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<InvoiceEntity> invoicePage = invoiceReadRepository.findByCurrency(currency, pageable);

        List<InvoiceResponse> invoices = invoicePage.getContent().stream()
                .map(InvoiceResponse::from)
                .collect(Collectors.toList());

        return PageResponse.of(
                invoices,
                page,
                size,
                invoicePage.getTotalElements()
        );
    }
}
