package com.droid.bss.application.query.invoice;

import com.droid.bss.application.dto.invoice.InvoiceDto;
import com.droid.bss.domain.invoice.InvoiceRepository;
import org.springframework.stereotype.Component;

/**
 * Stub class for GetInvoiceByIdUseCase
 * Minimal implementation for testing purposes
 */
@Component
public class GetInvoiceByIdUseCase {

    private final InvoiceRepository invoiceRepository;

    public GetInvoiceByIdUseCase(InvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }

    public InvoiceDto handle(String invoiceId) {
        // Stub implementation
        System.out.println("Getting invoice by ID: " + invoiceId);
        return null;
    }
}
