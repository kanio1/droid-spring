package com.droid.bss.application.query.invoice;

import com.droid.bss.application.dto.invoice.InvoiceDto;
import com.droid.bss.domain.invoice.InvoiceRepository;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Stub class for GetInvoicesByCustomerUseCase
 * Minimal implementation for testing purposes
 */
@Component
public class GetInvoicesByCustomerUseCase {

    private final InvoiceRepository invoiceRepository;

    public GetInvoicesByCustomerUseCase(InvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }

    public List<InvoiceDto> handle(String customerId) {
        // Stub implementation
        System.out.println("Getting invoices for customer: " + customerId);
        return List.of();
    }
}
