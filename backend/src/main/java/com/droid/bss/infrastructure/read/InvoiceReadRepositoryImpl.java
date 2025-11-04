package com.droid.bss.infrastructure.read;

import com.droid.bss.domain.invoice.InvoiceEntity;
import com.droid.bss.domain.invoice.InvoiceReadRepository;
import com.droid.bss.domain.invoice.InvoiceStatus;
import com.droid.bss.domain.invoice.InvoiceType;
import com.droid.bss.domain.invoice.repository.InvoiceEntityRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * JPA implementation of InvoiceReadRepository (read-side port)
 */
@Repository
public class InvoiceReadRepositoryImpl implements InvoiceReadRepository {

    private final InvoiceEntityRepository invoiceEntityRepository;

    public InvoiceReadRepositoryImpl(InvoiceEntityRepository invoiceEntityRepository) {
        this.invoiceEntityRepository = invoiceEntityRepository;
    }

    @Override
    public java.util.Optional<InvoiceEntity> findById(UUID id) {
        return invoiceEntityRepository.findById(id);
    }

    @Override
    public java.util.Optional<InvoiceEntity> findByInvoiceNumber(String invoiceNumber) {
        return invoiceEntityRepository.findByInvoiceNumber(invoiceNumber);
    }

    @Override
    public Page<InvoiceEntity> findAll(Pageable pageable) {
        return invoiceEntityRepository.findAll(pageable);
    }

    @Override
    public Page<InvoiceEntity> findByStatus(InvoiceStatus status, Pageable pageable) {
        return invoiceEntityRepository.findByStatus(status, pageable);
    }

    @Override
    public Page<InvoiceEntity> findByInvoiceType(InvoiceType invoiceType, Pageable pageable) {
        return invoiceEntityRepository.findByInvoiceType(invoiceType, pageable);
    }

    @Override
    public Page<InvoiceEntity> findByCustomerId(UUID customerId, Pageable pageable) {
        return invoiceEntityRepository.findByCustomerId(customerId, pageable);
    }

    @Override
    public Page<InvoiceEntity> findByIssueDateBetween(LocalDate startDate, LocalDate endDate, Pageable pageable) {
        return invoiceEntityRepository.findByIssueDateBetween(startDate, endDate, pageable);
    }

    @Override
    public Page<InvoiceEntity> findByDueDateBetween(LocalDate startDate, LocalDate endDate, Pageable pageable) {
        return invoiceEntityRepository.findByDueDateBetween(startDate, endDate, pageable);
    }

    @Override
    public Page<InvoiceEntity> findByStatusAndDueDateBefore(InvoiceStatus status, LocalDate date, Pageable pageable) {
        return invoiceEntityRepository.findByStatusAndDueDateBefore(status, date, pageable);
    }

    @Override
    public Page<InvoiceEntity> findByStatusIn(List<InvoiceStatus> statuses, Pageable pageable) {
        return invoiceEntityRepository.findByStatusIn(statuses, pageable);
    }

    @Override
    public Page<InvoiceEntity> findByTotalAmountBetween(BigDecimal minAmount, BigDecimal maxAmount, Pageable pageable) {
        return invoiceEntityRepository.findByTotalAmountBetween(minAmount, maxAmount, pageable);
    }

    @Override
    public Page<InvoiceEntity> findByCurrency(String currency, Pageable pageable) {
        return invoiceEntityRepository.findByCurrency(currency, pageable);
    }

    @Override
    public Page<InvoiceEntity> search(String query, Pageable pageable) {
        return invoiceEntityRepository.search(query, pageable);
    }
}
