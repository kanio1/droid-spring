package com.droid.bss.infrastructure.write;

import com.droid.bss.domain.invoice.InvoiceEntity;
import com.droid.bss.domain.invoice.InvoiceRepository;
import com.droid.bss.domain.invoice.InvoiceStatus;
import com.droid.bss.domain.invoice.InvoiceType;
import com.droid.bss.domain.invoice.repository.InvoiceEntityRepository;
import com.droid.bss.domain.invoice.event.InvoiceEventPublisher;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * JPA implementation of InvoiceRepository (write-side port)
 */
@Repository
@Transactional
public class InvoiceRepositoryImpl implements InvoiceRepository {

    private final InvoiceEntityRepository invoiceEntityRepository;
    private final InvoiceEventPublisher eventPublisher;

    public InvoiceRepositoryImpl(InvoiceEntityRepository invoiceEntityRepository,
                                  InvoiceEventPublisher eventPublisher) {
        this.invoiceEntityRepository = invoiceEntityRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public InvoiceEntity save(InvoiceEntity invoice) {
        InvoiceStatus previousStatus = invoice.getStatus();
        InvoiceEntity saved = invoiceEntityRepository.save(invoice);

        // Publish events based on operation
        if (previousStatus == null) {
            // New invoice
            eventPublisher.publishInvoiceCreated(saved);
        } else if (!previousStatus.equals(saved.getStatus())) {
            // Status changed
            eventPublisher.publishInvoiceStatusChanged(saved, previousStatus);
        } else {
            // General update
            eventPublisher.publishInvoiceUpdated(saved);
        }

        return saved;
    }

    @Override
    public java.util.Optional<InvoiceEntity> findById(java.util.UUID id) {
        return invoiceEntityRepository.findById(id);
    }

    @Override
    public java.util.Optional<InvoiceEntity> findByInvoiceNumber(String invoiceNumber) {
        return invoiceEntityRepository.findByInvoiceNumber(invoiceNumber);
    }

    @Override
    public List<InvoiceEntity> findAll(int page, int size) {
        // Use pagination but return List for compatibility with port interface
        var pageable = org.springframework.data.domain.PageRequest.of(page / size, size);
        return invoiceEntityRepository.findAll(pageable).getContent();
    }

    @Override
    public List<InvoiceEntity> findByStatus(InvoiceStatus status, int page, int size) {
        var pageable = org.springframework.data.domain.PageRequest.of(page / size, size);
        return invoiceEntityRepository.findByStatus(status, pageable).getContent();
    }

    @Override
    public List<InvoiceEntity> findByInvoiceType(InvoiceType invoiceType, int page, int size) {
        var pageable = org.springframework.data.domain.PageRequest.of(page / size, size);
        return invoiceEntityRepository.findByInvoiceType(invoiceType, pageable).getContent();
    }

    @Override
    public List<InvoiceEntity> findByCustomerId(UUID customerId, int page, int size) {
        var pageable = org.springframework.data.domain.PageRequest.of(page / size, size);
        return invoiceEntityRepository.findByCustomerId(customerId, pageable).getContent();
    }

    @Override
    public List<InvoiceEntity> findByIssueDateBetween(LocalDate startDate, LocalDate endDate, int page, int size) {
        var pageable = org.springframework.data.domain.PageRequest.of(page / size, size);
        return invoiceEntityRepository.findByIssueDateBetween(startDate, endDate, pageable).getContent();
    }

    @Override
    public List<InvoiceEntity> findByDueDateBetween(LocalDate startDate, LocalDate endDate, int page, int size) {
        var pageable = org.springframework.data.domain.PageRequest.of(page / size, size);
        return invoiceEntityRepository.findByDueDateBetween(startDate, endDate, pageable).getContent();
    }

    @Override
    public List<InvoiceEntity> findByTotalAmountBetween(BigDecimal minAmount, BigDecimal maxAmount, int page, int size) {
        var pageable = org.springframework.data.domain.PageRequest.of(page / size, size);
        return invoiceEntityRepository.findByTotalAmountBetween(minAmount, maxAmount, pageable).getContent();
    }

    @Override
    public List<InvoiceEntity> findByCurrency(String currency, int page, int size) {
        var pageable = org.springframework.data.domain.PageRequest.of(page / size, size);
        return invoiceEntityRepository.findByCurrency(currency, pageable).getContent();
    }

    @Override
    public List<InvoiceEntity> search(String query, int page, int size) {
        var pageable = org.springframework.data.domain.PageRequest.of(page / size, size);
        return invoiceEntityRepository.search(query, pageable).getContent();
    }

    @Override
    public long count() {
        return invoiceEntityRepository.count();
    }

    @Override
    public long countByStatus(InvoiceStatus status) {
        return invoiceEntityRepository.countByStatus(status);
    }

    @Override
    public boolean deleteById(java.util.UUID id) {
        invoiceEntityRepository.deleteById(id);
        return true;
    }
}
